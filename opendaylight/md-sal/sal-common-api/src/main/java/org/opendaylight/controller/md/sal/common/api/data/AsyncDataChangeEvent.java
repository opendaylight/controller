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
 * Data Change Event which contains state capture of changes in data subtree,
 *
 * Data Change Event istriggered by successful application of modifications from
 * transactions on global data tree.
 *
 * This data change events are generated and triggered purely on subscription
 * via
 * {@link AsyncDataBroker#registerDataChangeListener(LogicalDatastoreType, Path, AsyncDataChangeListener, org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope)}
 * which consumers invoke in order to subscribe for particular subtree of data
 * tree for data change events.
 *
 * Data Change event does not contain state of whole data tree, but only state
 * for subtree for which it was generated.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncDataChangeEvent<P extends Path<P>, D> extends Immutable {
    /**
     * Returns a immutable map of paths and newly created objects
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getCreatedData();

    /**
     * Returns a immutable map of paths and respective updated objects after
     * update.
     *
     * Original state of the object is in {@link #getOriginalData()}
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getUpdatedData();

    /**
     * Returns a immutable set of removed paths.
     *
     * Original state of the object is in {@link #getOriginalData()}
     *
     * @return set of removed paths
     */
    Set<P> getRemovedPaths();

    /**
     * Return a immutable map of paths and original state of updated and removed
     * objects.
     *
     * This map is populated if at changed path was previous object, and
     * captures state of previous object.
     *
     * @return map of paths and original state of updated and removed objects.
     */
    Map<P, ? extends D> getOriginalData();

    /**
     * Returns a immutable stable view of data state, which captures state of
     * data store before the reported change.
     *
     *
     * The view is rooted at the point where the listener, to which the event is
     * being delivered, was registered.
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
     * @return Stable view of data after the change happened, rooted at the
     *         listener registration path.
     */
    D getUpdatedSubtree();
}
