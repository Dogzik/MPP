package fgbank;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bank implementation.
 *
 */
public class BankImpl implements Bank {
    /**
     * An array of accounts by index.
     */
    private final Account[] accounts;

    /**
     * Creates new bank instance.
     *
     * @param n the number of accounts (numbered from 0 to n-1).
     */
    public BankImpl(int n) {
        accounts = new Account[n];
        for (int i = 0; i < n; i++) {
            accounts[i] = new Account();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfAccounts() {
        return accounts.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAmount(int index) {
        try {
            accounts[index].lock();
            return accounts[index].amount;
        } finally {
            accounts[index].unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalAmount() {
        long sum = 0;
        int maxLock = -1;
        try {
            for (int i = 0; i < accounts.length; ++i) {
                accounts[i].lock();
                maxLock = i;
                sum += accounts[i].amount;
            }
            return sum;
        } finally {
            for (int i = maxLock; i >= 0; --i) {
                accounts[i].unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long deposit(int index, long amount) {
        try {
            accounts[index].lock();
            if (amount <= 0) {
                throw new IllegalArgumentException("Invalid amount: " + amount);
            }
            Account account = accounts[index];
            if (amount > MAX_AMOUNT || account.amount + amount > MAX_AMOUNT) {
                throw new IllegalStateException("Overflow");
            }
            account.amount += amount;
            return account.amount;
        } finally {
            accounts[index].unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long withdraw(int index, long amount) {
        try {
            accounts[index].lock();
            if (amount <= 0) {
                throw new IllegalArgumentException("Invalid amount: " + amount);
            }
            Account account = accounts[index];
            if (account.amount - amount < 0) {
                throw new IllegalStateException("Underflow");
            }
            account.amount -= amount;
            return account.amount;
        } finally {
            accounts[index].unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transfer(int fromIndex, int toIndex, long amount) {
        int indArray[] = {Math.min(fromIndex, toIndex), Math.max(fromIndex, toIndex)};
        int maxLockInd = -1;
        try {
            for (int i = 0; i < indArray.length; ++i) {
                accounts[indArray[i]].lock();
                maxLockInd = i;
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("Invalid amount: " + amount);
            }
            if (fromIndex == toIndex) {
                throw new IllegalArgumentException("fromIndex == toIndex");
            }
            Account from = accounts[fromIndex];
            Account to = accounts[toIndex];
            if (amount > from.amount) {
                throw new IllegalStateException("Underflow");
            } else if (amount > MAX_AMOUNT || to.amount + amount > MAX_AMOUNT) {
                throw new IllegalStateException("Overflow");
            }
            from.amount -= amount;
            to.amount += amount;
        } finally {
            for (int i = maxLockInd; i >= 0; --i) {
                accounts[indArray[i]].unlock();
            }
        }
    }

    /**
     * Private account data structure.
     */
    private static class Account {
        /**
         * Amount of funds in this account.
         */
        long amount;

        /**
         * Lock for thread-safety
         */
        private Lock lock;

        Account() {
            amount = 0;
            lock = new ReentrantLock();
        }

        final void lock() {
            lock.lock();
        }

        final void unlock() {
            lock.unlock();
        }
    }
}
