package faaqueue;


import com.devexperts.dxlab.lincheck.LinChecker;
import com.devexperts.dxlab.lincheck.LoggingLevel;
import com.devexperts.dxlab.lincheck.annotations.LogLevel;
import com.devexperts.dxlab.lincheck.annotations.Operation;
import com.devexperts.dxlab.lincheck.annotations.Param;
import com.devexperts.dxlab.lincheck.paramgen.IntGen;
import com.devexperts.dxlab.lincheck.strategy.stress.StressCTest;
import org.junit.Test;


@StressCTest
@LogLevel(LoggingLevel.DEBUG)
public class LinearizabilityTest {
    private Queue<Integer> queue = new FAAQueue<>();

    @Operation
    public void enqueue(@Param(gen = IntGen.class) Integer x) {
        queue.enqueue(x);
    }

    @Operation
    public Integer dequeue() {
        return queue.dequeue();
    }

    @Test
    public void test() {
        LinChecker.check(LinearizabilityTest.class);
    }
}