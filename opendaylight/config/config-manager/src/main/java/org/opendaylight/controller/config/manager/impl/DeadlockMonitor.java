/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadlockMonitor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DeadlockMonitor.class);

    private static final long WARN_AFTER_MILLIS = 5000;

    private final TransactionIdentifier transactionIdentifier;
    private final DeadlockMonitorRunnable thread;
    @GuardedBy("this")
    private final Deque<ModuleIdentifierWithNanos> moduleIdentifierWithNanosStack = new LinkedList<>();
    @GuardedBy("this")
    private ModuleIdentifierWithNanos top = ModuleIdentifierWithNanos.EMPTY;

    public DeadlockMonitor(TransactionIdentifier transactionIdentifier) {
        this.transactionIdentifier = transactionIdentifier;
        thread = new DeadlockMonitorRunnable();
        thread.start();
    }

    public synchronized void setCurrentlyInstantiatedModule(ModuleIdentifier currentlyInstantiatedModule) {

        boolean popping = currentlyInstantiatedModule == null;
        if (popping) {
            moduleIdentifierWithNanosStack.pop();
            if (moduleIdentifierWithNanosStack.isEmpty()) {
                top = ModuleIdentifierWithNanos.EMPTY;
            } else {
                top = moduleIdentifierWithNanosStack.peekLast();
            }
        } else {
            ModuleIdentifierWithNanos current = new ModuleIdentifierWithNanos(currentlyInstantiatedModule);
            moduleIdentifierWithNanosStack.push(current);
            top = current;
        }
        LOG.trace("setCurrentlyInstantiatedModule {}, top {}", currentlyInstantiatedModule, top);
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
                ModuleIdentifierWithNanos copy;
                synchronized(this) {
                    copy = new ModuleIdentifierWithNanos(DeadlockMonitor.this.top);
                }

                if (old.moduleIdentifier == null || old.equals(copy) == false) {
                    // started
                    old = copy;
                } else {
                    // is the getInstance() running longer than WARN_AFTER_MILLIS ?
                    long runningTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - copy.nanoTime);
                    if (runningTime > WARN_AFTER_MILLIS) {
                        LOG.warn("{} did not finish after {} ms", copy.moduleIdentifier, runningTime);
                    }
                }
                try {
                    sleep(WARN_AFTER_MILLIS);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
            LOG.trace("Exiting {}", this);
        }

        @Override
        public String toString() {
            return "DeadLockMonitorRunnable{" + transactionIdentifier + "}";
        }
    }




    private static class ModuleIdentifierWithNanos {
        private static ModuleIdentifierWithNanos EMPTY = new ModuleIdentifierWithNanos();
        @Nullable
        private final ModuleIdentifier moduleIdentifier;

        private final long nanoTime;

        private ModuleIdentifierWithNanos() {
            this((ModuleIdentifier)null);
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModuleIdentifierWithNanos that = (ModuleIdentifierWithNanos) o;

            if (nanoTime != that.nanoTime) {
                return false;
            }
            if (moduleIdentifier != null ? !moduleIdentifier.equals(that.moduleIdentifier) : that.moduleIdentifier != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = moduleIdentifier != null ? moduleIdentifier.hashCode() : 0;
            result = 31 * result + (int) (nanoTime ^ (nanoTime >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "ModuleIdentifierWithNanos{" +
                    moduleIdentifier +
                    '}';
        }
    }
}
