/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.Path;

/**
 *
 * An event which contains a capture of changes in a data subtree
 *
 * <p>
 * Represents a notification indicating that some data at or under a particular
 * path has changed. The notification contains a capture of the changes in the data
 * subtree. This event is triggered by successful application of modifications
 * from a transaction on the global data tree. Use the
 * {@link AsyncDataBroker#registerDataChangeListener(LogicalDatastoreType, Path, AsyncDataChangeListener, AsyncDataBroker.DataChangeScope)}
 * method to register a listener for data change events.
 *
 * <p>
 * A listener will only receive notifications for changes to data under the path
 * they register for. See
 * {@link AsyncDataBroker#registerDataChangeListener(LogicalDatastoreType, Path, AsyncDataChangeListener, AsyncDataBroker.DataChangeScope)}
 * to learn more about registration scopes.
 *
 * <p>
 * The entire subtree under the path will be provided via instance methods of Data
 * Change Event even if just a leaf node changes.
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
public interface AsyncDataChangeEvent<P extends Path<P>, D> extends Immutable {
    /**
     * Returns a map of paths and newly created objects, which were introduced by
     * this change into conceptual data tree, if no new objects were introduced
     * this map will be empty.
     *<p>
     * This map contains all data tree nodes (and paths to them) which were created
     * and are in  the scope of listener registration. The data tree nodes
     * contain their whole subtree with their current state.
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getCreatedData();

    /**
     * Returns a map of paths and objects which were updated by this change in the
     * conceptual data tree if no existing objects were updated
     * this map will be empty.
     *<p>
     * This map contains all data tree nodes (and paths to them) which were updated
     * and are in the scope of listener registration. The data tree nodes
     * contain their whole subtree with their current state.
     *<p>
     * A Node is considered updated if it contents were replaced or one of its
     * children was created, removed or updated.
     *<p>
     * Original state of the updated data tree nodes is in
     * {@link #getOriginalData()} stored with same path.
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getUpdatedData();

    /**
     * Returns an immutable set of removed paths.
     *<p>
     * This set contains the paths to the data tree nodes which are in the scope
     * of the listener registration that have been removed.
     *<p>
     * Original state of the removed data tree nodes is in
     * {@link #getOriginalData()} stored with same path.
     *
     * @return set of removed paths
     */
    Set<P> getRemovedPaths();

    /**
     * Returns an immutable map of updated or removed paths and their original
     * states prior to this change.
     *
     *<p>
     * This map contains the original version of the data tree nodes (and paths
     * to them), which are in the scope of the listener registration.
     *
     * @return map of paths and original state of updated and removed objects.
     */
    Map<P, D> getOriginalData();

    /**
     * Returns an immutable stable view of data state, which captures the state of
     * data store before the reported change.
     *
     *<p>
     * The view is rooted at the point where the listener, to which the event is
     * being delivered, was registered.
     *<p>
     * If listener used a wildcarded path (if supported by path type) during
     * registration for change listeners this method returns null, and original
     * state can be accessed only via {@link #getOriginalData()}
     *
     * @return Stable view of data before the change happened, rooted at the
     *         listener registration path.
     *
     */
    D getOriginalSubtree();

    /**
     * Returns an immutable stable view of data, which captures the state of data
     * store after the reported change.
     *<p>
     * The view is rooted at the point where the listener, to which the event is
     * being delivered, was registered.
     *<p>
     * If listener used a wildcarded path (if supported by path type) during
     * registration for change listeners this method returns null, and state
     * can be accessed only via {@link #getCreatedData()},
     * {@link #getUpdatedData()}, {@link #getRemovedPaths()}
     *
     * @return Stable view of data after the change happened, rooted at the
     *         listener registration path.
     */
    D getUpdatedSubtree();
}
