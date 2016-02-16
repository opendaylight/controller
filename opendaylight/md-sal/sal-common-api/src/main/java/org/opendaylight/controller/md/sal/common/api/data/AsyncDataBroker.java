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
 * Base interface that provides access to a conceptual data tree store and also provides the ability to
 * subscribe for changes to data under a given branch of the tree.
 *
 * <p>
 * All operations on the data tree are performed via one of the transactions:
 * <ul>
 * <li>Read-Only - allocated using {@link #newReadOnlyTransaction()}
 * <li>Write-Only - allocated using {@link #newWriteOnlyTransaction()}
 * <li>Read-Write - allocated using {@link #newReadWriteTransaction()}
 * </ul>
 *
 * <p>
 * These transactions provide a stable isolated view of data tree, which is
 * guaranteed to be not affected by other concurrent transactions, until
 * transaction is committed.
 *
 * <p>
 * For a detailed explanation of how transaction are isolated and how transaction-local
 * changes are committed to global data tree, see
 * {@link AsyncReadTransaction}, {@link AsyncWriteTransaction},
 * {@link AsyncReadWriteTransaction} and {@link AsyncWriteTransaction#commit()}.
 *
 *
 * <p>
 * It is strongly recommended to use the type of transaction, which
 * provides only the minimal capabilities you need. This allows for
 * optimizations at the data broker / data store level. For example,
 * implementations may optimize the transaction for reading if they know ahead
 * of time that you only need to read data - such as not keeping additional meta-data,
 * which may be required for write transactions.
 *
 * <p>
 * <b>Implementation Note:</b> This interface is not intended to be implemented
 * by users of MD-SAL, but only to be consumed by them.
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
     * <p>
     * Represents scope of data change (addition, replacement, deletion).
     *
     * The terminology for scope types is reused from LDAP.
     *
     * <h2>Examples</h2>
     *
     * Following is an example model with comments describing what notifications
     * you would receive based on the scope you specify, when you are
     * registering for changes on container a.
     *
     * <pre>
     * container a              // scope BASE, ONE, SUBTREE
     *    leaf "foo"            // scope ONE, SUBTREE
     *    container             // scope ONE, SUBTREE
     *       leaf  "bar"        // scope SUBTREE
     *    list list             // scope ONE, SUBTREE
     *      list [a]            // scope SUBTREE
     *        id "a"            // scope SUBTREE
     *      list [b]            // scope SUBTREE
     *        id "b"            // scope SUBTREE
     * </pre>
     *
     * Following is an example model with comments describing what notifications
     * you would receive based on the scope you specify, when you are
     * registering for changes on list list (without specifying concrete item in
     * the list).
     *
     * <pre>
     *  list list               // scope BASE, ONE, SUBTREE
     *      list [a]            // scope ONE, SUBTREE
     *        id "a"            // scope SUBTREE
     *      list [b]            // scope ONE, SUBTREE
     *        id "b"            // scope SUBTREE
     * </pre>
     *
     *
     * @see http://www.idevelopment.info/data/LDAP/LDAP_Resources/
     *      SEARCH_Setting_the_SCOPE_Parameter.shtml
     */
    public enum DataChangeScope {

        /**
         * Represents only a direct change of the node, such as replacement of a node, addition or
         * deletion. Note that, as described in {@link #ONE}, this may have counterintuitive
         * interactions when viewed from a <i>binding aware</i> application, in particular when it
         * pertains to lists.
         *
         */
        BASE,
        /**
         * Represent a change (addition,replacement,deletion) of the node or one of its direct
         * children.
         * <p>
         * Note that this is done in the <i>binding independent</i> data tree and so the behavior
         * might be counterintuitive when used with <i>binding aware</i> interfaces particularly
         * when it comes to lists. The list itself is a node in the <i>binding independent</i> tree,
         * which means that if you want to listen on new elements you must listen on the list itself
         * with the scope of {@link #ONE}.
         * <p>
         * As an example, in the below YANG snippet, listening on <tt>node</tt> with scope
         * {@link #ONE} would tell you if the <tt>node-connector</tt> list was created or deleted,
         * but not when elements were added or removed from the list assuming the list itself
         * already existed.
         *
         * <pre>
         * container nodes {
         *   list node {
         *     key "id";
         *     leaf id {
         *       type string;
         *     }
         *     list node-connector {
         *       leaf id {
         *         type string;
         *       }
         *     }
         *   }
         * }
         * </pre>
         *
         * This scope is superset of {@link #BASE}.
         *
         */
        ONE,
        /**
         * Represents a change of the node or any of or any of its child nodes,
         * direct and nested.
         *
         * This scope is superset of {@link #ONE} and {@link #BASE}.
         *
         */
        SUBTREE
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncReadOnlyTransaction<P, D> newReadOnlyTransaction();

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
     * Registers a {@link AsyncDataChangeListener} to receive
     * notifications when data changes under a given path in the conceptual data
     * tree.
     * <p>
     * You are able to register for notifications  for any node or subtree
     * which can be reached via the supplied path.
     * <p>
     * If path type <code>P</code> allows it, you may specify paths up to the leaf nodes
     * then it is possible to listen on leaf nodes.
     * <p>
     * You are able to register for data change notifications for a subtree even
     * if it does not exist. You will receive notification once that node is
     * created.
     * <p>
     * If there is any preexisting data in data tree on path for which you are
     * registering, you will receive initial data change event, which will
     * contain all preexisting data, marked as created.
     *
     * <p>
     * You are also able to specify the scope of the changes you want to be
     * notified.
     * <p>
     * Supported scopes are:
     * <ul>
     * <li>{@link DataChangeScope#BASE} - notification events will only be
     * triggered when a node referenced by path is created, removed or replaced.
     * <li>{@link DataChangeScope#ONE} - notifications events will only be
     * triggered when a node referenced by path is created, removed or replaced,
     * or any or any of its immediate children are created, updated or removed.
     * <li>{@link DataChangeScope#SUBTREE} - notification events will be
     * triggered when a node referenced by the path is created, removed
     * or replaced or any of the children in its subtree are created, removed
     * or replaced.
     * </ul>
     * See {@link DataChangeScope} for examples.
     * <p>
     * This method returns a {@link ListenerRegistration} object. To
     * "unregister" your listener for changes call the "close" method on this
     * returned object.
     * <p>
     * You MUST call close when you no longer need to receive notifications
     * (such as during shutdown or for example if your bundle is shutting down).
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
     * @return Listener registration object, which may be used to unregister
     *         your listener using {@link ListenerRegistration#close()} to stop
     *         delivery of change events.
     */
    ListenerRegistration<L> registerDataChangeListener(LogicalDatastoreType store, P path, L listener,
            DataChangeScope triggeringScope);
}
