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
 * Data Change Event which contains capture of changes in data subtree
 *
 * Represents a notification indicating that some data at or under a particular
 * path has changed. The notification contains a capture of the changes in data
 * subtree. This event is triggered by successful application of modifications
 * from a transaction on the global data tree. Use the
 * {@link AsyncDataBroker#registerDataChangeListener(LogicalDatastoreType, Path, AsyncDataChangeListener, org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope)}
 * method to register a listener for data change events.
 *
 * A listener will only
 * receive notifications for changes to data under the path they register for.
 *
 * The entire subtree under the path is be provided via instance methods of Data Change Event
 * even if just a leaf node changes.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncDataChangeEvent<P extends Path<P>, D> extends Immutable {
    /**
     * Returns a map of paths and newly created objects, which was introduced
     * by this change into conceptual data tree, if no new objects
     * were introduced this map will be empty.
     *
     * This map contains all data tree nodes (and paths to them),
     * which was in scope of listener registration and was currently
     * created. This data tree nodes contains their whole subtree
     * with current state.
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getCreatedData();

    /**
     * Returns a map of paths and updated objects, which was changed
     * by this change into conceptual data tree, if no existing objects
     * were updated this map will be empty.
     *
     * This map contains all data tree nodes (and paths to them),
     * which was in scope of listener registration and was currently
     * updated. This data tree nodes contains their whole subtree
     * with current state.
     *
     * Node is considered updated if it contents was replaced one of
     * its children was created, removed or updated.
     *
     * Original state of the updated data tree nodes is in {@link #getOriginalData()}
     * stored with same path.
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getUpdatedData();

    /**
     * Returns a immutable set of removed paths.
     *
     * This set contains path data tree nodes (and paths to them),
     * which was in scope of listener registration and was currently
     * updated. This data tree nodes contains their whole subtree
     * with current state.
     *
     * Original state of the removed data tree nodes is
     * in {@link #getOriginalData()} stored with same path.
     *
     * @return set of removed paths
     */
    Set<P> getRemovedPaths();

    /**
     * Return a immutable map of paths and original state of updated and removed
     * objects.
     *
     * This map contains all original version of data tree nodes (and paths to them),
     * which was in scope of listener registration and was currently updated.
     *
     * @return map of paths and original state of updated and removed objects.
     */
    Map<P, D> getOriginalData();

    /**
     * Returns a immutable stable view of data state, which captures state of
     * data store before the reported change.
     *
     * The view is rooted at the point where the listener, to which the event is
     * being delivered, was registered.
     *
     * If listener used wildcarded path (if supported by path type) during
     * registration for change listeners this method returns null, and original
     * state could be accessed only via {@link #getOriginalData()}
     *
     * @return Stable view of data before the change happened, rooted at the
     *         listener registration path.
     *
     */
    D getOriginalSubtree();

    /**
     * Returns a immutable stable view of data, which captures state of data
     * store after the reported change.
     *
     * The view is rooted at the point where the listener, to which the event is
     * being delivered, was registered.
     *
     * If listener used wildcarded path (if supported by path type) during
     * registration for change listeners this method returns null, and
     * state could be accessed only via {@link #getCreatedData()},
     * {@link #getUpdatedData()}, {@link #getRemovedPaths()}
     *
     * @return Stable view of data after the change happened, rooted at the
     *         listener registration path.
     */
    D getUpdatedSubtree();
}
