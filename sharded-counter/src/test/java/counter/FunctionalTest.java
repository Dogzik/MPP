package counter;

import org.junit.Test;

import java.util.Random;
import java.util.Stack;

import static org.junit.Assert.assertEquals;

public class FunctionalTest {

    @Test
    public void test() {
        Counter c = new CounterImpl();
        assertEquals(0, c.get());
        c.inc();
        assertEquals(1, c.get());
        c.inc();
        c.inc();
        assertEquals(3, c.get());
    }
}