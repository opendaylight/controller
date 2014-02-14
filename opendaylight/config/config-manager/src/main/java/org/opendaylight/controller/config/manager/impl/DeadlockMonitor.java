package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.TimeUnit;

public class DeadlockMonitor implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DeadlockMonitorRunnable.class);

    private static final long WARN_AFTER_MILLIS = 5000;

    private final TransactionIdentifier transactionIdentifier;
    private final DeadlockMonitorRunnable thread;
    @GuardedBy("this")
    private ModuleIdentifierWithNanos moduleIdentifierWithNanos = new ModuleIdentifierWithNanos();

    public DeadlockMonitor(TransactionIdentifier transactionIdentifier) {
        this.transactionIdentifier = transactionIdentifier;
        thread = new DeadlockMonitorRunnable();
        thread.start();
    }

    public synchronized void setCurrentlyInstantiatedModule(ModuleIdentifier currentlyInstantiatedModule) {
        this.moduleIdentifierWithNanos = new ModuleIdentifierWithNanos(currentlyInstantiatedModule);
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    @Override
    public void close() {
        thread.interrupt();
    }

    @Override
    public String toString() {
        return "DeadlockMonitor{" + transactionIdentifier + '}';
    }

    private class DeadlockMonitorRunnable extends Thread {

        private DeadlockMonitorRunnable() {
            super(DeadlockMonitor.this.toString());
        }

        @Override
        public void run() {
            ModuleIdentifierWithNanos old = new ModuleIdentifierWithNanos(); // null moduleId
            while (this.isInterrupted() == false) {
                ModuleIdentifierWithNanos copy = new ModuleIdentifierWithNanos(DeadlockMonitor.this.moduleIdentifierWithNanos);
                if (old.moduleIdentifier == null) {
                    // started
                    old = copy;
                } else if (old.moduleIdentifier != null && old.equals(copy)) {
                    // is the getInstance() running longer than WARN_AFTER_MILLIS ?
                    long runningTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - copy.nanoTime);
                    if (runningTime > WARN_AFTER_MILLIS) {
                        logger.warn("{} did not finish after {} ms", copy.moduleIdentifier, runningTime);
                    }
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
            logger.trace("Exiting {}", this);
        }

        @Override
        public String toString() {
            return "DeadLockMonitorRunnable{" + transactionIdentifier + "}";
        }
    }

    private class ModuleIdentifierWithNanos {
        @Nullable
        private final ModuleIdentifier moduleIdentifier;
        private final long nanoTime;

        private ModuleIdentifierWithNanos() {
            moduleIdentifier = null;
            nanoTime = System.nanoTime();
        }

        private ModuleIdentifierWithNanos(ModuleIdentifier moduleIdentifier) {
            this.moduleIdentifier = moduleIdentifier;
            nanoTime = System.nanoTime();
        }

        private ModuleIdentifierWithNanos(ModuleIdentifierWithNanos copy) {
            moduleIdentifier = copy.moduleIdentifier;
            nanoTime = copy.nanoTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModuleIdentifierWithNanos that = (ModuleIdentifierWithNanos) o;

            if (nanoTime != that.nanoTime) return false;
            if (moduleIdentifier != null ? !moduleIdentifier.equals(that.moduleIdentifier) : that.moduleIdentifier != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = moduleIdentifier != null ? moduleIdentifier.hashCode() : 0;
            result = 31 * result + (int) (nanoTime ^ (nanoTime >>> 32));
            return result;
        }
    }
}
