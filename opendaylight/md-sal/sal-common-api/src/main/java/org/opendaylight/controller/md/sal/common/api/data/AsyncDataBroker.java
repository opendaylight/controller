/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Path;

public interface AsyncDataBroker<P extends Path<P>, D, L extends AsyncDataChangeListener<P, D>> extends //
        AsyncDataTransactionFactory<P, D> {

    /**
     *
     * Scope of Data Change
     *
     * Represents scope of data change (addition, replacement, deletion).
     *
     * The terminology for types is reused from LDAP
     *
     * @see http://www.idevelopment.info/data/LDAP/LDAP_Resources/SEARCH_Setting_the_SCOPE_Parameter.shtml
     */
    public enum DataChangeScope {

       /**
        * Represents only a direct change of the node, such as replacement of node,
        * addition or deletion.
        *
        */
       BASE,
       /**
        * Represent a change (addition,replacement,deletion)
        * of the node or one of it's direct childs.
        *
        */
       ONE,
       /**
        * Represents a change of the node or any of it's child nodes.
        *
        */
       SUBTREE
    }

    @Override
    public AsyncReadTransaction<P, D> newReadOnlyTransaction();

    @Override
    public AsyncReadWriteTransaction<P,D> newReadWriteTransaction();

    @Override
    public AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

    /**
     * Registers {@link DataChangeListener} for Data Change callbacks
     * which will be triggered on which will be triggered on the store
     *
     * @param store Logical store in which listener is registered.
     * @param path Path (subtree identifier) on which client listener will be invoked.
     * @param listener Instance of listener which should be invoked on
     * @param triggeringScope Scope of change which triggers callback.
     * @return Listener registration of the listener, call {@link ListenerRegistration#close()}
     *         to stop delivery of change events.
     */
    ListenerRegistration<L> registerDataChangeListener(LogicalDatastoreType store, P path, L listener, DataChangeScope triggeringScope);
}
