package stack;

import java.util.EmptyStackException;
import java.util.concurrent.atomic.AtomicReference;

public class StackImpl implements Stack {
    private class Node {
        Node next;
        int x;

        Node(int x, Node next) {
            this.next = next;
            this.x = x;
        }
    }

    private final Node NIL = new Node(0, null);
    private AtomicReference<Node> head = new AtomicReference<>(NIL);

    @Override
    public void push(int x) {
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
