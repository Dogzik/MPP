package linked_list_set;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class SetImpl implements Set {
    private class Node {
        final AtomicMarkableReference<Node> next;
        final int x;

        Node(int x, Node next, boolean mark) {
            this.next = new AtomicMarkableReference<>(next, mark);
            this.x = x;
        }
    }

    private class Window {
        final Node cur, next;

        Window(Node cur, Node next) {
            this.cur = cur;
            this.next = next;
        }
    }

    private final Node head = new Node(Integer.MIN_VALUE,
            new Node(Integer.MAX_VALUE, null, false), false);

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        boolean[] nextMarkHolder = new boolean[1];
        while (true) {
            inner:
            {
                Node cur = head;
                Node next = cur.next.getReference();
                while (next.x < x) {
                    Node nextNext = next.next.get(nextMarkHolder);
                    if (nextMarkHolder[0]) {
                        if (cur.next.compareAndSet(next, nextNext, false, false)) {
                            next = nextNext;
                        } else {
                            break inner;
                        }
                    } else {
                        cur = next;
                        next = cur.next.getReference();
                    }
                }
                Node nextNext = next.next.get(nextMarkHolder);
                if (nextMarkHolder[0]) {
                    cur.next.compareAndSet(next, nextNext, false, false);
                    continue;
                }
                return new Window(cur, next);
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) {
                return false;
            }
            Node newNode = new Node(x, w.next, false);
            if (w.cur.next.compareAndSet(w.next, newNode, false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            } else {
                Node nextNext = w.next.next.getReference();
                if (w.next.next.compareAndSet(nextNext, nextNext, false, true)) {
                    w.cur.next.compareAndSet(w.next, nextNext, false, false);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}