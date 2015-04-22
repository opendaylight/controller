/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract DOM Store Transaction.
 *
 * Convenience super implementation of DOM Store transaction which provides
 * common implementation of {@link #toString()} and {@link #getIdentifier()}.
 *
 * It can optionally capture the context where it was allocated.
 *
 * <T> identifier type
 */
@Beta
public abstract class AbstractDOMStoreTransaction<T> implements DOMStoreTransaction {
    private final Throwable debugContext;
    private final T identifier;

    protected AbstractDOMStoreTransaction(@Nonnull final T identifier) {
        this(identifier, false);
    }

    protected AbstractDOMStoreTransaction(@Nonnull final T identifier, final boolean debug) {
        this.identifier = Preconditions.checkNotNull(identifier, "Identifier must not be null.");
        this.debugContext = debug ? new Throwable().fillInStackTrace() : null;
    }

    @Override
    public final T getIdentifier() {
        return identifier;
    }

    /**
     * Return the context in which this transaction was allocated.
     *
     * @return The context in which this transaction was allocated, or null
     *         if the context was not recorded.
     */
    @Nullable
    public final Throwable getDebugContext() {
        return debugContext;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    /**
     * Add class-specific toString attributes.
     *
     * @param toStringHelper
     *            ToStringHelper instance
     * @return ToStringHelper instance which was passed in
     */
    protected ToStringHelper addToStringAttributes(@Nonnull final ToStringHelper toStringHelper) {
        return toStringHelper.add("id", identifier);
    }
}
