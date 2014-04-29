/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.listener;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;

final class UncancellableFuture<V> extends AbstractFuture<V> {
    @GuardedBy("this")
    private boolean uncancellable = false;

    public UncancellableFuture(final boolean uncancellable) {
        this.uncancellable = uncancellable;
    }

    public synchronized boolean setUncancellable() {
        if (isCancelled()) {
            return false;
        }

        uncancellable = true;
        return true;
    }

    public synchronized boolean isUncancellable() {
        return uncancellable;
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        return uncancellable ? false : super.cancel(mayInterruptIfRunning);
    }

    @Override
    public synchronized boolean set(@Nullable final V value) {
        Preconditions.checkState(uncancellable);
        return super.set(value);
    }

    @Override
    protected boolean setException(final Throwable throwable) {
        Preconditions.checkState(uncancellable);
        return super.setException(throwable);
    }
}
