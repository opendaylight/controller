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

/**
 *
 * Data Broker which provides access to a conceptual data tree store.
 *
 * Also provides the ability to subscribe for changes to data under a given
 * branch of the tree.
 *
 * All operations on data tree are performed via one of the transactions:
 * <ul>
 * <li>Read-Only - allocated using {@link #newReadOnlyTransaction()}
 * <li>Write-Only - allocated using {@link #newWriteOnlyTransaction()}
 * <li>Read-Write - allocated using {@link #newReadWriteTransaction()}
 * </ul>
 *
 * It is strongly recommended to use the type of transaction, which which
 * provides only the minimal capabilities you need. This allows for
 * optimizations at the data broker / data store level. For example,
 * implementations may optimize the transaction for reading if they know ahead
 * of time that you only need to read data in such way, that they do not need to
 * keep additional metadata, which may be required for write transactions.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
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
     * @see http://www.idevelopment.info/data/LDAP/LDAP_Resources/
     *      SEARCH_Setting_the_SCOPE_Parameter.shtml
     */
    public enum DataChangeScope {

        /**
         * Represents only a direct change of the node, such as replacement of
         * node, addition or deletion.
         *
         */
        BASE,
        /**
         * Represent a change (addition,replacement,deletion) of the node or one
         * of it's direct childs.
         *
         */
        ONE,
        /**
         * Represents a change of the node or any of it's child nodes.
         *
         */
        SUBTREE
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncReadTransaction<P, D> newReadOnlyTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

    /**
     * Registers a {@DataChangeListener} to receive notifications when data changes
     * under the given path in conceptual data tree.
     * <p>
     * You are able to register for notifications for any node / subtree,
     * which is possible to reference via supplied path type.
     * <p>
     * If path type allows to specify path up to the leaf nodes, it is possible
     * to listen on leaf nodes.
     * <p>
     * You are able to register for data change notifications for subtree
     * even if it does not exists and you will receive notification once that
     * node is created.
     * <p>
     * If there are any preexisting data in data tree on path for which
     * you are registering, you will receive initial data change event,
     * which will contains all preexisting data marked as created.
     *
     * <p>
     * You are also able to specify scope of the changes you want to be notified.
     * <p>
     * Supported scopes are:
     * <ul>
     * <li>{@link DataChangeScope#BASE} - notification events will be only triggered
     * when node referenced by path is created, removed or replaced.
     * <li>{@link DataChangeScope#ONE} - notifications events will be only triggered
     * when node referenced by path is created, removed or replaced, or any of
     * it's direct children is created, removed or replaced.
     * <li>{@link DataChangeScope#SUBTREE} - notification events will be triggered
     * for any change of node path is referencing or any change of children nodes.
     * </ul>
     *<p>
     * This method returns a {@link ListenerRegistration} object. To "unregister" your
     * listener for changes call the "close" method on this returned object.
     *<p>
     * You SHOULD call close when you no longer need to receive notifications (such
     * as during shutdown or for example if your bundle is shutting down).
     *
     * @param store
     *            Logical Data Store - Logical Datastore you want to listen for
     *            changes in. For example
     *            {@link LogicalDatastoreType#OPERATIONAL} or
     *            {@link LogicalDatastoreType#CONFIGURATION}
     * @param path
     *            Path (subtree identifier) on which client listener will be
     *            invoked.
     * @param listener
     *            Instance of listener which should be invoked on
     * @param triggeringScope
     *            Scope of change which triggers callback.
     * @return Listener registration object, which may be used to
     *          unregister your listener using
     *         {@link ListenerRegistration#close()} to stop delivery of change
     *         events.
     */
    ListenerRegistration<L> registerDataChangeListener(LogicalDatastoreType store, P path, L listener,
            DataChangeScope triggeringScope);
}
