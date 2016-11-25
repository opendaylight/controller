/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Stopwatch;
import java.lang.UnsupportedOperationException;  // remove when no longer needed

// This class has a style close to java.util.concurrent, e.g. "e" instead of "element".

/**
 * FIXME: Add proper javadoc!
 */
public class ArrayThrottlingQueue<E> extends ArrayBlockingQueue<E> {

    // FIXME: Generate serialVersionUID, the following line is from ArrayBlockingQueue.
    // private static final long serialVersionUID = -817911632652898426L;

    /**
     * For rate estimation we need total number or dequeued elements.
     */
    protected long takeCount;

    /**
     * Stopawtch counts aggregare time spent non-empty.
     */
    protected final Stopwatch stopwatch;

    /**
     * We need to track tick of last enqueue.
     */
    protected long lastPut;

    /**
     * Provide an estimate on a number of ticks from now until an offer will be accepted.
     *
     * <p>Only call while holding lock.
     *
     * @return how many ticks until an offer would pass if nothing changes, may be negative
     */
    protected long estimateBlockingTicks() {
        if (count <= items.length / 2) {
            return 0L;
        }

        final long runtime = stopwatch.elapsedNanos();
        final double ticksPerElement = (runtime > 0L ? 1.0 / takeCount * runtime : 1L);
        final double fullness = 1.0 * count / items.length;  // relative fullness
        final double scaledWait = ticksPerElement * (fullness - 1.0 / 2.0) / (1.0 - fullness);
        // fullness => scaledWait: 1/2 => 0; 3/4 => ticksPerElement; 1 => +infinity;
        return lastPut - runtime + ((long) scaledWait);
    }

    // TODO: Extract the preceding rate prediction and wait time computation into a separate class.

    /**
     * Inserts element at current put position, advances, and signals.
     * Call only when holding lock.
     */
    protected void enqueue(E e) {
        super.enqueue(e);
        lastPut = stopwatch.elapsedNanos();
        if (count == 1) {
            stopwatch.start();
        }
    }

    /**
     * Extracts element at current take position, advances, and signals.
     * Call only when holding lock.
     */
    protected E dequeue() {
        if (count == 1) {
            stopwatch.stop();
        }
        takeCount++;
        return super.dequeue();
    }

    /**
     * Creates an {@code ArrayThrottlingQueue} with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public ArrayThrottlingQueue(int capacity) {
        this(capacity, false);
    }

    /**
     * Creates an {@code ArrayThrottlingQueue} with the given (fixed)
     * capacity and the specified access policy.
     *
     * @param capacity the capacity of this queue
     * @param fair if {@code true} then queue accesses for threads blocked
     *        on insertion or removal, are processed in FIFO order;
     *        if {@code false} the access order is unspecified.
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public ArrayBlockingQueue(int capacity, boolean fair) {
        super(capacity, fair);
        this.egressCount = 0L;
        this.stopwatch = Stopwatch.createUnstarted();
        this.lastPut = stopwatch.elapsedNanos();  // the first element will be accepted anyway
        // TODO: Contructors with an explicit Ticker?
    }

    // TODO: Do we need a constructor with initial collection?

    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's rate reserve,
     * returning {@code true} upon success and {@code false} if this queue
     * is currently throttled. This method is generally preferable to method {@link #add},
     * which can fail to insert an element only by throwing an exception.
     *
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length) {
                return false;
            }
            if (estimateBlockingTicks() <= 0L) {
                this.enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's rate reserve,
     * returning {@code true} upon success and throwing an
     * {@code IllegalStateException} if this queue is currently throttled.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if this queue is currently throttled
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return super.add(e);  // traverses to ArrayBlockingQueue#offer(e)
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting
     * for unblock if the queue is currently throttled.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length || estimateBlockingTicks() > 0L)
                notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * FIXME: Update.
     *
     * Inserts the specified element at the tail of this queue, waiting
     * up to the specified wait time for space to become available if
     * the queue is full.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        throw UnsupportedOperationException();
    }

    // The poll/take/peek methods are inherited from ArrayBlockingQueue without any issues.
}