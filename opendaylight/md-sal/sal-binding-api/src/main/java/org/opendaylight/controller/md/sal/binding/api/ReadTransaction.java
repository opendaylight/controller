/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public interface ReadTransaction extends AsyncReadTransaction<InstanceIdentifier<?>, DataObject> {

    /**
     * {@inheritDoc}
     */
    @Override
    ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path);

    /**
     * Type-safe version of {@link #read(LogicalDatastoreType, InstanceIdentifier)} method.
     *
     * @param store
     *            Logical data store from which read should occur.
     * @param path
     *            Path which uniquely identifies subtree which client want to
     *            read
     * @return Listenable Future which contains read result
     *         <ul>
     *         <li>If data at supplied path exists the
     *         {@link ListeblaFuture#get()} returns Optional object containing
     *         data once read is done.
     *         <li>If data at supplied path does not exists the
     *         {@link ListenbleFuture#get()} returns {@link Optional#absent()}.
     *         </ul>
     *
     */
    <T extends DataObject> ListenableFuture<Optional<T>> readChecked(LogicalDatastoreType store, InstanceIdentifier<T> path);
}
