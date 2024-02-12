import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Iuzeev, Artur
 */
class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E) {
        if (combinerLock.compareAndSet(false, true)) {
            queue.addLast(element)
            runTasksOnArray()
            combinerLock.set(false)
            return
        }

        val indexCell = getCell(element, true)
        while (true) {
            if (combinerLock.compareAndSet(false, true)) {
                val task = tasksForCombiner.get(indexCell)
                if (task !is Result<*>) {
                    queue.addLast(element)
                    tasksForCombiner.set(indexCell, null)
                    runTasksOnArray()
                } else {
                    tasksForCombiner.set(indexCell, null)
                }
                combinerLock.set(false)
                break
            }
        }
    }

    override fun dequeue(): E? {
        if (combinerLock.compareAndSet(false, true)) {
            val res = queue.removeFirstOrNull()
            runTasksOnArray()
            combinerLock.set(false)
            return res
        }

        val indexCell = getCell(null, false)
        while (true) {
            if (combinerLock.compareAndSet(false, true)) {
                val task = tasksForCombiner.get(indexCell)
                if (task !is Result<*>) {
                    val result = queue.removeFirstOrNull()
                    tasksForCombiner.set(indexCell, null)
                    runTasksOnArray()
                    combinerLock.set(false)
                    return result
                } else {
                    combinerLock.set(false)
                    val result = (task as Result<E?>).value
                    tasksForCombiner.set(indexCell, null)
                    return result
                }
            }
        }
    }

    private fun getCell(element: E?, flag: Boolean): Int {
        var indexCell = randomCellIndex()
        while (true) {
            if ((flag && tasksForCombiner.compareAndSet(
                    indexCell,
                    null,
                    element
                )) || (!flag && tasksForCombiner.compareAndSet(indexCell, null, Dequeue))
            ) {
                return indexCell
            }
            indexCell = randomCellIndex()
        }
    }

    fun runTasksOnArray() {
        for (i in 0..<tasksForCombiner.length()) {
            val element = tasksForCombiner.get(i)
            if (element is Result<*> || element == null) {
                continue
            }
            if (element is Dequeue) {
                tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                continue
            }
            queue.addLast(element as E)
            tasksForCombiner.set(i, Result(null))
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
private object Dequeue

// TODO: Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
private class Result<V>(
    val value: V
)