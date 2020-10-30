package lock_free_hashtable;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Int-to-Int hash map with open addressing and linear probes.
 */
public class IntIntHashMap {
    private static final int MAGIC = 0x9E3779B9; // golden ratio
    private static final int INITIAL_CAPACITY = 2; // !!! DO NOT CHANGE INITIAL CAPACITY !!!
    private static final int MAX_PROBES = 8; // max number of probes to find an item

    private static final int NULL_KEY = 0; // missing key (initial value)
    private static final int NULL_VALUE = 0; // missing value (initial value)
    private static final int DEL_VALUE = Integer.MAX_VALUE; // mark for removed value
    private static final int NEEDS_REHASH = -1; // returned by putInternal to indicate that rehash is needed
    private static final int LOOK_FOR_NEXT = -2; // returned by putInternal to indicate that needed lookup in other table
    private static final int MOVED_VALUE = Integer.MIN_VALUE; // value moved during rehashing to new table

    // Checks is the value is in the range of allowed values
    private static boolean isValue(int value) {
        return value > 0 && value < DEL_VALUE; // the range or allowed values
    }

    // Converts internal value to the public results of the methods
    private static int toValue(int value) {
        return isValue(value) ? value : 0;
    }

    private final AtomicReference<Core> core = new AtomicReference<>(new Core(INITIAL_CAPACITY));

    /**
     * Returns value for the corresponding key or zero if this key is not present.
     *
     * @param key a positive key.
     * @return value for the corresponding or zero if this key is not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    public int get(int key) {
        if (key <= 0) {
            throw new IllegalArgumentException("Key must be positive: " + key);
        }
        return toValue(core.get().getInternal(key));
    }

    /**
     * Changes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key   a positive key.
     * @param value a positive value.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key or value are not positive, or value is equal to
     *                                  {@link Integer#MAX_VALUE} which is reserved.
     */
    public int put(int key, int value) {
        if (key <= 0) {
            throw new IllegalArgumentException("Key must be positive: " + key);
        }
        if (!isValue(value)) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
        return toValue(putAndRehashWhileNeeded(key, value));
    }

    /**
     * Removes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key a positive key.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    public int remove(int key) {
        if (key <= 0) {
            throw new IllegalArgumentException("Key must be positive: " + key);
        }
        return toValue(putAndRehashWhileNeeded(key, DEL_VALUE));
    }

    private int putAndRehashWhileNeeded(int key, int value) {
        while (true) {
            Core curCore = core.get();
            int oldValue = curCore.putInternal(key, value, false);
            if (oldValue == NEEDS_REHASH) {
                core.compareAndSet(curCore, curCore.rehash());
            } else if (oldValue == LOOK_FOR_NEXT) {
                core.compareAndSet(curCore, curCore.next.get());
            } else {
                return oldValue;
            }
        }
    }

    static private boolean isMarkedValue(int value) {
        return ((value & Integer.MIN_VALUE) != 0);
    }

    static private int getUnmarkedValue(int value) {
        return (value & (~Integer.MIN_VALUE));
    }

    static private int getMarkedValue(int value) {
        return (value | Integer.MIN_VALUE);
    }

    private static class Core {
        final AtomicIntegerArray map; // pairs of (key, value) here
        final int shift;
        final AtomicReference<Core> next;

        /**
         * Creates new core with a given capacity for (key, value) pair.
         * The actual size of the map is twice as big.
         */
        Core(int capacity) {
            map = new AtomicIntegerArray(2 * capacity);
            int mask = capacity - 1;
            assert mask > 0 && (mask & capacity) == 0 : "Capacity must be power of 2: " + capacity;
            shift = 32 - Integer.bitCount(mask);
            next = new AtomicReference<>();
        }

        int getInternal(int key) {
            int index = index(key);
            int probes = 0;
            while (true) {
                int curKey = map.get(index);
                int curValue = map.get(index + 1);
                if (curValue == MOVED_VALUE) {
                    helpRehash(index + 2);
                    return next.get().getInternal(key);
                }
                if (isMarkedValue(curValue)) {
                    helpRehash(index);
                    //helpRehashOne(index);
                    return (curKey == key) ? getUnmarkedValue(curValue) : next.get().getInternal(key);
                }
                if (curKey == NULL_KEY) {
                    return NULL_VALUE;
                }
                if (curKey == key) {
                    return curValue;
                }
                if (++probes >= MAX_PROBES) {
                    return NULL_VALUE;
                }
                if (index == 0) {
                    index = map.length();
                }
                index -= 2;
            }
        }

        private void helpRehashOne(int index) {
            Core curNext = next.get();
            int curKey = map.get(index);
            int curValue = map.get(index + 1);
            if (isMarkedValue(curValue)) {
                int curRealValue = getUnmarkedValue(curValue);
                Core curRealNext = curNext;
                boolean moved = false;
                while (!moved) {
                    switch (curRealNext.putInternal(curKey, curRealValue, true)) {
                        case NEEDS_REHASH: {
                            curRealNext = curRealNext.rehash();
                            break;
                        }
                        case LOOK_FOR_NEXT: {
                            curRealNext = curRealNext.next.get();
                            break;
                        }
                        default: {
                            moved = true;
                            break;
                        }
                    }
                }
                map.compareAndSet(index + 1, curValue, MOVED_VALUE);
            }
        }

        private void helpRehash(int index) {
            Core curNext = next.get();
            while (index < map.length()) {
                int curKey = map.get(index);
                int curValue = map.get(index + 1);
                if (curValue == MOVED_VALUE) {
                    index += 2;
                    continue;
                }
                if (curKey == NULL_KEY || curValue == NULL_VALUE || curValue == DEL_VALUE) {
                    if (map.compareAndSet(index + 1, curValue, MOVED_VALUE)) {
                        index += 2;
                    }
                    continue;
                }
                if (!isMarkedValue(curValue)) {
                    if (!map.compareAndSet(index + 1, curValue, getMarkedValue(curValue))) {
                        continue;
                    }
                    curValue = getMarkedValue(curValue);
                }
                int curRealValue = getUnmarkedValue(curValue);
                Core curRealNext = curNext;
                boolean moved = false;
                while (!moved) {
                    switch (curRealNext.putInternal(curKey, curRealValue, true)) {
                        case NEEDS_REHASH: {
                            curRealNext = curRealNext.rehash();
                            break;
                        }
                        case LOOK_FOR_NEXT: {
                            curRealNext = curRealNext.next.get();
                            break;
                        }
                        default: {
                            moved = true;
                            break;
                        }
                    }
                }
                map.compareAndSet(index + 1, curValue, MOVED_VALUE);
                index += 2;
            }
        }

        int putInternal(int key, int value, boolean once) {
            int index = index(key);
            int probes = 0;
            int curKey = map.get(index);
            int curValue = map.get(index + 1);
            for (; curKey != key; curKey = map.get(index), curValue = map.get(index + 1)) {
                if (curValue == MOVED_VALUE) {
                    helpRehash(index + 2);
                    return LOOK_FOR_NEXT;
                }
                if (isMarkedValue(curValue)) {
                    helpRehash(index);
                    //helpRehashOne(index);
                    return LOOK_FOR_NEXT;
                }
                if (curKey == NULL_KEY) {
                    if (map.compareAndSet(index, NULL_KEY, key) || (map.get(index) == key)) {
                        break;
                    } else {
                        continue;
                    }
                }
                if (++probes >= MAX_PROBES) {
                    return NEEDS_REHASH;
                }
                if (index == 0) {
                    index = map.length();
                }
                index -= 2;
            }
            while (true) {
                if (curValue == MOVED_VALUE) {
                    helpRehash(index + 2);
                    return LOOK_FOR_NEXT;
                } else if (isMarkedValue(curValue)) {
                    helpRehash(index);
                    //helpRehashOne(index);
                    return once ? NULL_VALUE : LOOK_FOR_NEXT;
                } else if (curValue == DEL_VALUE) {
                    if (once || map.compareAndSet(index + 1, DEL_VALUE, value)) {
                        return NULL_VALUE;
                    }
                } else if (curValue == NULL_VALUE) {
                    if (map.compareAndSet(index + 1, NULL_VALUE, value)) {
                        return NULL_VALUE;
                    }
                } else {
                    if (once || map.compareAndSet(index + 1, curValue, value)) {
                        return curValue;
                    }
                }
                curValue = map.get(index + 1);
            }

        }

        Core rehash() {
            Core res = next.get();
            if (res == null) {
                res = new Core(map.length());
                if (!next.compareAndSet(null, res)) {
                    res = next.get();
                }
            }
            helpRehash(0);
            return res;
        }

        /**
         * Returns an initial index in map to look for a given key.
         */
        int index(int key) {
            return ((key * MAGIC) >>> shift) * 2;
        }
    }
}