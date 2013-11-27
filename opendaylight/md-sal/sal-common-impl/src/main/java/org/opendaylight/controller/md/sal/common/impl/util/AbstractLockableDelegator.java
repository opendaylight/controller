package org.opendaylight.controller.md.sal.common.impl.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.opendaylight.yangtools.concepts.Delegator;

import com.google.common.base.Preconditions;

public class AbstractLockableDelegator<T> implements Delegator<T> {

    private final ReentrantReadWriteLock delegateLock = new ReentrantReadWriteLock();
    private final ReadLock delegateReadLock = delegateLock.readLock();
    private final WriteLock delegateWriteLock = delegateLock.writeLock();
    
    
    protected Lock getDelegateReadLock() {
        return delegateReadLock;
    }

    private T delegate;

    public AbstractLockableDelegator() {
        // NOOP
    }

    public AbstractLockableDelegator(T initialDelegate) {
        delegate = initialDelegate;
    }

    @Override
    public T getDelegate() {
        try {
            delegateReadLock.lock();
            return delegate;
        } finally {
            delegateReadLock.unlock();
        }
    }

    public T retrieveDelegate() {
        try {
            delegateReadLock.lock();
            Preconditions.checkState(delegate != null,"Delegate is null");
            return delegate;
        } finally {
            delegateReadLock.unlock();
        }
    }

    /**
     * 
     * @param newDelegate
     * @return oldDelegate
     */
    public final T changeDelegate(T newDelegate) {
        try {
            delegateWriteLock.lock();
            T oldDelegate = delegate;
            delegate = newDelegate;
            onDelegateChanged(oldDelegate, newDelegate);
            return oldDelegate;
        } finally {
            delegateWriteLock.unlock();
        }
    }
    
    
    protected void onDelegateChanged(T oldDelegate, T newDelegate) {
        // NOOP in abstract calss;
    }
}
