import java.util.concurrent.atomic.*

/**
 * @author TODO: Iuzeev, Artur
 *
 * TODO: Copy the code from `FAABasedQueueSimplified`
 * TODO: and implement the infinite array on a linked list
 * TODO: of fixed-size `Segment`s.
 */
class FAABasedQueue<E> : Queue<E> {
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>

    init {
        val dummy = Segment(0)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val cur = tail.get()
            val i = enqIdx.getAndIncrement()
            val s = findSegment(cur, i / SEGMENT_SIZE)
            moveTailForward(s)
            val index = (i % SEGMENT_SIZE).toInt()
            if (s.cells.compareAndSet(index, null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (deqIdx.get() >= enqIdx.get()) return null
            val cur = head.get()
            val i = deqIdx.getAndIncrement()
            val s = findSegment(cur, i / SEGMENT_SIZE)
            moveHeadForward(s)
            if (s.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, POISONED)) {
                continue
            }
            return s.cells.get((i % SEGMENT_SIZE).toInt()) as E
        }
    }

    private fun findSegment(segment: Segment, index: Long) : Segment {
        var cur = segment
        while (index > cur.id) {
            cur.next.compareAndSet(null, Segment(cur.id + 1))
            cur = cur.next.get()!!
        }
        return cur
    }
    private fun moveTailForward(segment: Segment) {
        var cur = tail.get()
        while (cur.id < segment.id) {
            tail.compareAndSet(cur, segment)
            cur = tail.get()
        }
    }
    private fun moveHeadForward(segment: Segment) {
        var cur = head.get()
        while (cur.id < segment.id) {
            head.compareAndSet(cur, segment)
            cur = head.get()
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
private val POISONED = Any()