package lock_free_hashtable;

import com.devexperts.dxlab.lincheck.LinChecker;
import com.devexperts.dxlab.lincheck.LoggingLevel;
import com.devexperts.dxlab.lincheck.annotations.LogLevel;
import com.devexperts.dxlab.lincheck.annotations.Operation;
import com.devexperts.dxlab.lincheck.annotations.Param;
import com.devexperts.dxlab.lincheck.paramgen.IntGen;
import com.devexperts.dxlab.lincheck.strategy.stress.StressCTest;
import org.junit.Test;

@StressCTest(actorsPerThread = 15)
@Param(name = "key", gen = IntGen.class, conf = "1:6")
@Param(name = "value", gen = IntGen.class, conf = "1:100")
@LogLevel(LoggingLevel.DEBUG)
public class IntIntHashMapConcurrentTest {

    private IntIntHashMap map = new IntIntHashMap();

    @Operation(params = {"key", "value"})
    public Integer put(Integer key, Integer value) {
        return map.put(key, value);
    }

    @Operation(params = "key")
    public Integer remove(Integer key) {
        return map.remove(key);
    }

    @Operation(params = "key")
    public Integer get(Integer key) {
        return map.get(key);
    }

    @Test
    public void test() throws Exception {
        LinChecker.check(IntIntHashMapConcurrentTest.class);
    }
}
