package stack;

import sun.misc.Contended;

import java.util.EmptyStackException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class StackImpl implements Stack {
    private static class Node {
        Node next;
        int x;

        Node(int x, Node next) {
            this.next = next;
            this.x = x;
        }
    }

    private interface Cell {
    }

    private final static class CellEmpty implements Cell {
    }

    private final static class CellDone implements Cell {
    }

    private final static class CellValue implements Cell {
        int value;

        CellValue(int x) {
            value = x;
        }
    }

    @Contended
    private final static class AtomicCell extends AtomicReference<Cell> {
        AtomicCell(Cell x) {
            super(x);
        }
    }

    private static final Node NIL = new Node(0, null);
    private static final Cell EMPTY_CELL = new CellEmpty();
    private static final Cell DONE_CELL = new CellDone();
    private AtomicReference<Node> head;
    private final int BUFF_SIZE;
    private final int PROBES_CNT;
    private final int SPIN_LENGTH;
    private final AtomicCell[] buff;

    StackImpl() {
        head = new AtomicReference<>(NIL);
        BUFF_SIZE = 2 * Runtime.getRuntime().availableProcessors();
        PROBES_CNT = 4;
        SPIN_LENGTH = 32;
        buff = new AtomicCell[BUFF_SIZE];
        for (int i = 0; i < BUFF_SIZE; ++i) {
            buff[i] = new AtomicCell(new CellEmpty());
        }
    }

    @Override
    public void push(int x) {
        Cell tmp = new CellValue(x);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int startInd = random.nextInt(BUFF_SIZE);
        for (int i = 0; i < PROBES_CNT; ++i) {
            int ind = (startInd + i) % BUFF_SIZE;
            Cell curCell = buff[ind].get();
            if ((curCell instanceof CellEmpty) && buff[ind].compareAndSet(curCell, tmp)) {
                for (int k = 0; k < SPIN_LENGTH; ++k) {
                    random.nextInt();
                }
                curCell = buff[ind].get();
                if ((curCell instanceof CellValue) && buff[ind].compareAndSet(curCell, EMPTY_CELL)) {
                    break;
                } else {
                    buff[ind].set(EMPTY_CELL);
                    return;
                }
            }
        }

        Node newHead = new Node(x, null);
        while (true) {
            Node curHead = head.get();
            newHead.next = curHead;
            if (head.compareAndSet(curHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int startInd = ThreadLocalRandom.current().nextInt(BUFF_SIZE);
        for (int i = 0; i < PROBES_CNT; ++i) {
            int ind = (startInd + i) % BUFF_SIZE;
            Cell curCell = buff[ind].get();
            if ((curCell instanceof CellValue) && buff[ind].compareAndSet(curCell, DONE_CELL)) {
                return ((CellValue) curCell).value;
            }
        }

        while (true) {
            Node curHead = head.get();
            if (curHead == NIL) {
                throw new EmptyStackException();
            }
            if (head.compareAndSet(curHead, curHead.next)) {
                return curHead.x;
            }
        }
    }
}
