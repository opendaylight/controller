/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMStore {

    /**
     *
     * Creates a read only transaction
     *
     * @return
     */
    DOMStoreReadTransaction newReadOnlyTransaction();

    /**
     * Creates write only transaction
     *
     * @return
     */
    DOMStoreWriteTransaction newWriteOnlyTransaction();

    /**
     * Creates Read-Write transaction
     *
     * @return
     */
    DOMStoreReadWriteTransaction newReadWriteTransaction();

    /**
     * Registers {@link DataChangeListener} for Data Change callbacks
     * which will be triggered on the change of provided subpath. What
     * constitutes a change depends on the @scope parameter.
     *
     * Listener upon registration receives an initial callback
     * {@link AsyncDataChangeListener#onDataChanged(org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent)}
     * which contains stable view of data tree at the time of registration.
     *
     * @param path Path (subtree identifier) on which client listener will be invoked.
     * @param listener Instance of listener which should be invoked on
     * @param scope Scope of change which triggers callback.
     * @return Listener Registration object, which client may use to close registration
     *         / interest on receiving data changes.
     *
     */
    <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            InstanceIdentifier path, L listener, DataChangeScope scope);

}
