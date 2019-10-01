/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Abstract DOM Store Transaction.
 *
 * <p>
 * Convenience super implementation of DOM Store transaction which provides
 * common implementation of {@link #toString()} and {@link #getIdentifier()}.
 *
 * <p>
 * It can optionally capture the context where it was allocated.
 *
 * @param <T> identifier type
 * @deprecated Use {@link org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTransaction} instead.
 */
@Deprecated(forRemoval = true)
@Beta
public abstract class AbstractDOMStoreTransaction<T> implements DOMStoreTransaction {
    private final Throwable debugContext;
    private final @NonNull T identifier;

    protected AbstractDOMStoreTransaction(final @NonNull T identifier) {
        this(identifier, false);
    }

    protected AbstractDOMStoreTransaction(final @NonNull T identifier, final boolean debug) {
        this.identifier = requireNonNull(identifier, "Identifier must not be null.");
        this.debugContext = debug ? new Throwable().fillInStackTrace() : null;
    }

    @Override
    public final T getIdentifier() {
        return identifier;
    }

    /**
     * Return the context in which this transaction was allocated.
     *
     * @return The context in which this transaction was allocated, or null if the context was not recorded.
     */
    public final @Nullable Throwable getDebugContext() {
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
    protected ToStringHelper addToStringAttributes(final @NonNull ToStringHelper toStringHelper) {
        return toStringHelper.add("id", identifier);
    }
}
