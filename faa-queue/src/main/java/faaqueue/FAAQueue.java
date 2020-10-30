package faaqueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static faaqueue.FAAQueue.Node.NODE_SIZE;


public class FAAQueue<T> implements Queue<T> {
    private final Cell<T> DONE = new CellEmpty<>(); // Marker for the "DONE" slot state; to avoid memory leaks. Non-static because java generics suck

    private final AtomicReference<Node<T>> head; // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private final AtomicReference<Node<T>> tail; // Tail pointer, similarly to the Michael-Scott queue

    public FAAQueue() {
        Node<T> dummy = new Node<>();
        head = new AtomicReference<>(dummy);
        tail = new AtomicReference<>(dummy);
    }

    @Override
    public void enqueue(T x) {
        CellValue<T> newElem = new CellValue<>(x);
        while (true) {
            Node<T> curTail = tail.get();
            int enqIdx = curTail.enqIdx.getAndIncrement();
            if (enqIdx >= NODE_SIZE) {
                Node<T> newTail = new Node<>(newElem);
                while (true) {
                    curTail = tail.get();
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail);
                        return;
                    } else {
                        tail.compareAndSet(curTail, curTail.next.get());
                    }
                }
            } else {
                if (curTail.data.compareAndSet(enqIdx, null, newElem)) {
                    return;
                }
            }
        }
    }

    @Override
    public T dequeue() {
        while (true) {
            Node<T> curHead = head.get();
            Node<T> curTail = tail.get();
            Node<T> curHeadNext = curHead.next.get();
            if (curHead.isEmpty()) {
                if (curHead == curTail) {
                    if (curHeadNext == null) {
                        return null;
                    } else {
                        tail.compareAndSet(curTail, curHeadNext);
                    }
                } else {
                    head.compareAndSet(curHead, curHeadNext);
                }
            } else {
                int deqIdx = curHead.deqIdx.getAndIncrement();
                if (deqIdx >= NODE_SIZE) {
                    continue;
                }
                Cell<T> res = curHead.data.getAndSet(deqIdx, DONE);
                if (res != null) {
                    // Can't take DONE from this swap
                    return ((CellValue<T>) res).value;
                }
            }
        }
    }

    interface Cell<E> {
    }

    private static class CellValue<E> implements Cell<E> {
        final E value;

        CellValue(E elem) {
            value = elem;
        }
    }

    private static class CellEmpty<E> implements Cell<E> {
    }

    static class Node<E> {
        static final int NODE_SIZE = 5000; // CHANGE ME FOR BENCHMARKING ONLY

        private AtomicReference<Node<E>> next;
        private final AtomicInteger enqIdx; // index for the next enqueue operation
        private final AtomicInteger deqIdx; // index for the next dequeue operation
        private final AtomicReferenceArray<Cell<E>> data;

        Node() {
            next = new AtomicReference<>(null);
            enqIdx = new AtomicInteger(0);
            deqIdx = new AtomicInteger(0);
            data = new AtomicReferenceArray<>(NODE_SIZE);
        }

        Node(CellValue<E> x) {
            next = new AtomicReference<>(null);
            enqIdx = new AtomicInteger(1);
            deqIdx = new AtomicInteger(0);
            data = new AtomicReferenceArray<>(NODE_SIZE);
            data.set(0, x);
        }

        private boolean isEmpty() {
            return this.deqIdx.get() >= this.enqIdx.get();
        }
    }
}