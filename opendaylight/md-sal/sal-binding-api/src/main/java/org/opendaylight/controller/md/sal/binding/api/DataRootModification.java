/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.binding.api;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Represent modification of conceptual root of data tree.
 *
 *
 * @author Tony Tkacik &lt;ttkacik@cisco.com&gt;
 *
 */
public interface DataRootModification {

    /**
     * Returns before state of top level container.
     *
     * @param root Class representing data container
     * @return State of data container before modification. Null if subtree is not present.
     */
    @Nullable <T extends ChildOf<? extends DataRoot>> T getRootBefore(Class<T> root);

    /**
     * Returns after state of top level container.
     *
     * @param root Class representing data container
     * @return State of data container after modification. Null if subtree is not present.
     */
    @Nullable <T extends ChildOf<? extends DataRoot>> T getRootAfter(Class<T> root);

    /**
     * Returns unmodifiable collection of modified direct children.
     *
     * @return unmodifiable collection of modified direct children.
     */
    @Nonnull Collection<DataObjectModification<?>> getModifiedChildren();

    /**
     * Returns modification of child identified by Instance Identifier.
     *
     * @param path Path to child
     * @return data modification of child, if child was modified in this modification, otherwise
     *         returns null.
     */
    @Nullable <T extends DataObject> DataObjectModification<T> getModifiedChild(InstanceIdentifier<T> path);
}
