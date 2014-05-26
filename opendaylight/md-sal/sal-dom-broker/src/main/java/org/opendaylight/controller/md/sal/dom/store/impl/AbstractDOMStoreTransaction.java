/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Abstract DOM Store Transaction
 *
 * Convenience super implementation of DOM Store transaction which provides
 * common implementation of {@link #toString()} and {@link #getIdentifier()}.
 *
 *
 */
abstract class AbstractDOMStoreTransaction implements DOMStoreTransaction {
    private final Object identifier;

    protected AbstractDOMStoreTransaction(final Object identifier) {
        this.identifier = Preconditions.checkNotNull(identifier,"Identifier must not be null.");
    }

    @Override
    public final Object getIdentifier() {
        return identifier;
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