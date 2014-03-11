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

public interface AsyncDataChangeEvent<P extends Path<P>,D> extends Immutable {

    /**
     * Returns a immutable map of paths and newly created objects
     *
     * @return map of paths and newly created objects
     */
    Map<P, D> getCreatedData();


    /**
     * Returns a map of paths and respective updated objects after update.
     *
     * Original state of the object is in
     * {@link #getOriginalOperationalData()}
     *
     * @return map of paths and newly created objects
     */

    Map<P, D> getUpdatedData();

    /**
     * Returns a set of paths of removed objects.
     *
     * Original state of the object is in
     * {@link #getOriginalData()}
     *
     * @return set of removed paths
     */
    Set<P> getRemovedData();

    /**
     * Return a map of paths and original state of updated and removed objects.
     *
     * @return map of paths and original state of updated and removed objects.
     */
    Map<P, ? extends D> getOriginalData();

    /**
     * Returns a original subtree of data, which starts at the path
     * where listener was registered.
     *
     */
    D getOriginalSubtree();

    /**
     * Returns a updated subtree of data, which starts at the path
     * where listener was registered.
     *
     */
    D getUpdatedSubtree();

}
