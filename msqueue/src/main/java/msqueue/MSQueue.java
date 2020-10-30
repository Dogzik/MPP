package msqueue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

public class MSQueue implements Queue {
    private AtomicReference<Node> head;
    private AtomicReference<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0, null);
        this.head = new AtomicReference<>(dummy);
        this.tail = new AtomicReference<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        while (true) {
            Node curTail = tail.get();
            if (curTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(curTail, newTail);
                return;
            } else {
                tail.compareAndSet(curTail, curTail.next.get());
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.get();
            Node curTail = tail.get();
            Node curHeadNext = curHead.next.get();
            if (curHead == curTail) {
                if (curHeadNext == null) {
                    throw new NoSuchElementException();
                } else {
                    tail.compareAndSet(curTail, curHeadNext);
                }
            } else {
                if (head.compareAndSet(curHead, curHeadNext)) {
                    return curHeadNext.x;
                }
            }
        }
    }

    @Override
    public int peek() {
       while (true) {
           Node curHead = head.get();
           Node curTail = tail.get();
           Node curHeadNext = curHead.next.get();
           if (curHead == curTail) {
               if (curHeadNext == null) {
                   throw new NoSuchElementException();
               } else {
                   tail.compareAndSet(curTail, curHeadNext);
               }
           } else {
               return curHeadNext.x;
           }
       }
    }

    private class Node {
        final int x;
        AtomicReference<Node> next;

        Node(int x, Node next) {
            this.x = x;
            this.next = new AtomicReference<>(next);
        }
    }
}