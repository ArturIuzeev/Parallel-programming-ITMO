import java.util.concurrent.atomic.*

/**
 * @author TODO: Iuzeev, Artur
 */
class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val cur = tail.get()
            val newItem = Node(element)
            if (cur.next.compareAndSet(null, newItem)) {
                tail.compareAndSet(cur, newItem)
                return
            } else {
                tail.compareAndSet(cur, cur.next.get())
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val cur = head.get()
            val curNext = cur.next.get()
            if (curNext == null) {
                return null
            }

            if (head.compareAndSet(cur, curNext)) {
                val res = curNext.element
                curNext.element = null
                return res
            }

        }
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "At the end of the execution, `tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "At the end of the execution, the dummy node shouldn't store an element"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
