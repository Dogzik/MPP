package counter;

import sun.misc.Contended;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CounterImpl implements Counter {
    @Contended
    private static final class ContentedAtomicInteger extends AtomicInteger {
        ContentedAtomicInteger() {
            super(0);
        }
    }

    private final int SHARDS_CNT;
    private final AtomicReferenceArray<ContentedAtomicInteger> values;

    CounterImpl() {
        SHARDS_CNT = 16 * Runtime.getRuntime().availableProcessors();
        values = new AtomicReferenceArray<>(SHARDS_CNT);
        for (int i = 0; i < SHARDS_CNT; ++i) {
            values.set(i, new ContentedAtomicInteger());
        }
    }

    @Override
    public void inc() {
        values.get(ThreadLocalRandom.current().nextInt(SHARDS_CNT)).getAndIncrement();
    }

    @Override
    public int get() {
        int res = 0;
        for (int i = 0; i < SHARDS_CNT; ++i) {
            res += values.get(i).get();
        }
        return res;
    }
}
