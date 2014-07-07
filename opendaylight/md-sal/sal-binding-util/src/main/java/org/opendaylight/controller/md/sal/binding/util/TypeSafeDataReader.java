/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.util;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 *
 *
 * @deprecated Use
 *             {@link org.opendaylight.controller.md.sal.binding.api.ReadTransaction#read(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType, InstanceIdentifier)}
 *             instead.
 */
@Deprecated
public final class TypeSafeDataReader {

    private final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> delegate;

    public DataReader<InstanceIdentifier<?>, DataObject> getDelegate() {
        return delegate;
    }

    public TypeSafeDataReader(
            final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    public <D extends DataObject> D readConfigurationData(
            final InstanceIdentifier<D> path) {
        return (D) delegate.readConfigurationData(path);
    }

    @SuppressWarnings("unchecked")
    public <D extends DataObject> D readOperationalData(
            final InstanceIdentifier<D> path) {
        return (D) delegate.readOperationalData(path);
    }

    public static TypeSafeDataReader forReader(
            final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> delegate) {
        return new TypeSafeDataReader(delegate);
    }
}
