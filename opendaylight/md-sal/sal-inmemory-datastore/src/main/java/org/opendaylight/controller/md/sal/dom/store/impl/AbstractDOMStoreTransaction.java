/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.dom.store.impl.jmx.InMemoryDataStoreTransactionStatsTracker;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.slf4j.Logger;

/**
 * Abstract DOM Store Transaction
 *
 * Convenience super implementation of DOM Store transaction which provides
 * common implementation of {@link #toString()} and {@link #getIdentifier()}.
 */
abstract class AbstractDOMStoreTransaction implements DOMStoreTransaction {
    private final Throwable debugContext;
    private final Object identifier;
    private final InMemoryDataStoreTransactionStatsTracker statsTracker;

    protected AbstractDOMStoreTransaction(final Object identifier,
            final InMemoryDataStoreTransactionStatsTracker statsTracker, final boolean debug) {
        this.identifier = Preconditions.checkNotNull(identifier, "Identifier must not be null.");
        this.debugContext = debug ? new Throwable().fillInStackTrace() : null;
        this.statsTracker = Preconditions.checkNotNull(statsTracker,"statsTracker must not be null.");
    }

    protected InMemoryDataStoreTransactionStatsTracker getStatsTracker() {
        return statsTracker;
    }

    @Override
    public final Object getIdentifier() {
        return identifier;
    }

    protected final void warnDebugContext(final Logger logger) {
        if (debugContext != null) {
            logger.warn("Transaction {} has been allocated in the following context", identifier, debugContext);
        }
    }

    @Override
    public final String toString() {
        return addToStringAttributes(Objects.toStringHelper(this)).toString();
    }

    /**
     * Add class-specific toString attributes.
     *
     * @param toStringHelper
     *            ToStringHelper instance
     * @return ToStringHelper instance which was passed in
     */
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("id", identifier);
    }
}