/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * A unique identifier for a particular subtree. It is composed of the logical
 * data store type and the instance identifier of the root node.
 */
public final class DOMDataTreeIdentifier implements Immutable, Path<DOMDataTreeIdentifier>, Serializable {
    private static final long serialVersionUID = 1L;
    private final YangInstanceIdentifier rootIdentifier;
    private final LogicalDatastoreType datastoreType;

    public DOMDataTreeIdentifier(final LogicalDatastoreType datastoreType, final YangInstanceIdentifier rootIdentifier) {
        this.datastoreType = Preconditions.checkNotNull(datastoreType);
        this.rootIdentifier = Preconditions.checkNotNull(rootIdentifier);
    }

    /**
     * Return the logical data store type.
     *
     * @return Logical data store type. Guaranteed to be non-null.
     */
    public @Nonnull LogicalDatastoreType getDatastoreType() {
        return datastoreType;
    }

    /**
     * Return the {@link YangInstanceIdentifier} of the root node.
     *
     * @return Instance identifier corresponding to the root node.
     */
    public @Nonnull YangInstanceIdentifier getRootIdentifier() {
        return rootIdentifier;
    }

    @Override
    public boolean contains(final DOMDataTreeIdentifier other) {
        return datastoreType == other.datastoreType && rootIdentifier.contains(other.rootIdentifier);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + datastoreType.hashCode();
        result = prime * result + rootIdentifier.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DOMDataTreeIdentifier)) {
            return false;
        }
        DOMDataTreeIdentifier other = (DOMDataTreeIdentifier) obj;
        if (datastoreType != other.datastoreType) {
            return false;
        }
        return rootIdentifier.equals(other.rootIdentifier);
    }
}
