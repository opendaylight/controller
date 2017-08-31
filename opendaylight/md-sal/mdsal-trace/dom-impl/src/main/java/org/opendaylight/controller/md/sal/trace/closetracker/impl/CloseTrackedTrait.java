/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Implementation of {@link CloseTracked} which can be used as a field in
 * another class which implements {@link CloseTracked} and delegates its methods
 * to this.
 *
 * <p>This is useful if that class already has another parent class.
 * If it does not, then it's typically more convenient to just extend AbstractCloseTracked.
 *
 * @author Michael Vorburger.ch
 */
public class CloseTrackedTrait<T extends CloseTracked<T>> implements CloseTracked<T> {

    // NB: It's important that we keep a Throwable here, and not directly the StackTraceElement[] !
    // This is because creating a new Throwable() is a lot less expensive in terms of runtime overhead
    // than actually calling its getStackTrace(), which we can delay until we really need to.
    // see also e.g. https://stackoverflow.com/a/26122232/421602
    private final @Nullable Throwable allocationContext;
    private final CloseTrackedRegistry<T> closeTrackedRegistry;
    private final CloseTracked<T> realCloseTracked;

    public CloseTrackedTrait(CloseTrackedRegistry<T> transactionChainRegistry, CloseTracked<T> realCloseTracked) {
        if (transactionChainRegistry.isDebugContextEnabled()) {
            // NB: We're NOT doing the (expensive) getStackTrace() here just yet (only below)
            // TODO When we're on Java 9, then instead use the new java.lang.StackWalker API..
            this.allocationContext = new Throwable();
        } else {
            this.allocationContext = null;
        }
        this.realCloseTracked = Objects.requireNonNull(realCloseTracked, "realCloseTracked");
        this.closeTrackedRegistry = Objects.requireNonNull(transactionChainRegistry, "transactionChainRegistry");
        this.closeTrackedRegistry.add(this);
    }

    @Override
    @Nullable
    public StackTraceElement[] getAllocationContextStackTrace() {
        return allocationContext != null ? allocationContext.getStackTrace() : null;
    }

    public void removeFromTrackedRegistry() {
        closeTrackedRegistry.remove(this);
    }

    @Override
    public CloseTracked<T> getRealCloseTracked() {
        return realCloseTracked;
    }

}
