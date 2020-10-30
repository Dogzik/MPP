package mutex;

import org.jetbrains.annotations.NotNull;

/**
 * Java stub for distributed mutual exclusion implementation.
 * All functions are called from the single main thread.
 */
public class ProcessJavaImpl implements Process {
    private final Environment env;

    public ProcessJavaImpl(Environment env) {
        this.env = env;
    }

    @Override
    public void onMessage(int srcId, @NotNull Message message) {
        /* todo: write implementation here */
    }

    @Override
    public void onLockRequest() {
        /* todo: write implementation here */
        env.locked();
    }

    @Override
    public void onUnlockRequest() {
        /* todo: write implementation here */
        env.unlocked();
    }
}
