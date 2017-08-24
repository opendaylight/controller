/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl;

import java.time.Instant;
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

    private final Instant created;
    private final @Nullable Throwable allocationContext;
    private final CloseTrackedRegistry<T> closeTrackedRegistry;

    public CloseTrackedTrait(CloseTrackedRegistry<T> transactionChainRegistry) {
        this.created = Instant.now();
        if (transactionChainRegistry.isDebugContextEnabled()) {
            this.allocationContext = new Throwable("allocated at");
        } else {
            this.allocationContext = null;
        }
        this.closeTrackedRegistry = Objects.requireNonNull(transactionChainRegistry, "transactionChainRegistry");
        this.closeTrackedRegistry.add(this);
    }

    @Override
    public Instant getObjectCreated() {
        return created;
    }

    @Override
    public Throwable getAllocationContext() {
        return allocationContext;
    }

    public void removeFromTrackedRegistry() {
        closeTrackedRegistry.remove(this);
    }

}
