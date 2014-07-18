/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A transaction that provides read access to a logical data store.
 * <p>
 * For more information on usage and examples, please see the documentation in {@link AsyncReadTransaction}.
 */
public interface DOMDataReadTransaction extends AsyncReadTransaction<InstanceIdentifier, NormalizedNode<?, ?>> {

    /**
     * Reads data from provided logical data store located at the provided path.
     *<p>
     * If the target is a subtree, then the whole subtree is read (and will be
     * accessible from the returned data object).
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
     */
    ListenableFuture<Optional<NormalizedNode<?,?>>> read(LogicalDatastoreType store,InstanceIdentifier path);
}
