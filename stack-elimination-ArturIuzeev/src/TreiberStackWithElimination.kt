import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author TODO: Iuzeev, Artur
 */
open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    // TODO: Try to optimize concurrent push and pop operations,
    // TODO: synchronizing them in an `eliminationArray` cell.
    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        // TODO: Choose a random cell in `eliminationArray`
        // TODO: and try to install the element there.
        // TODO: Wait `ELIMINATION_WAIT_CYCLES` loop cycles
        // TODO: in hope that a concurrent `pop()` grabs the
        // TODO: element. If so, clean the cell and finish,
        // TODO: returning `true`. Otherwise, move the cell
        // TODO: to the empty state and return `false`.
        val r = randomCellIndex()

        if (eliminationArray.compareAndSet(r, CELL_STATE_EMPTY, element)) {
            for (i in 0..ELIMINATION_WAIT_CYCLES) {
                if (eliminationArray.compareAndSet(r, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                    return true
                }
            }

            if (eliminationArray.compareAndSet(r, element, CELL_STATE_EMPTY)) {
                return false
            }

            eliminationArray.set(r, CELL_STATE_EMPTY)
            return true
        }
        return false
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        // TODO: Choose a random cell in `eliminationArray`
        // TODO: and try to retrieve an element from there.
        // TODO: On success, return the element.
        // TODO: Otherwise, if the cell is empty, return `null`.
        val r = randomCellIndex()
        val item = eliminationArray.get(r)
        return if (item == CELL_STATE_EMPTY || item == CELL_STATE_RETRIEVED) {
            null
        } else if (eliminationArray.compareAndSet(r, item, CELL_STATE_RETRIEVED)) {
            item as E
        } else null
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}
