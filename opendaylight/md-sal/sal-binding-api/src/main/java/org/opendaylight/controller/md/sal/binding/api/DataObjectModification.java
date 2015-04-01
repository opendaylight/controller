/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * Modified Data Object.
 *
 * Represents modification of Data Object.
 *
 */
public interface DataObjectModification<T extends DataObject> extends org.opendaylight.yangtools.concepts.Identifiable<PathArgument> {

    enum ModificationType {
        /**
         *
         * Child node (direct or indirect) was modified.
         *
         */
        SUBTREE_MODIFIED,
        /**
         *
         * Node was explicitly created / overwritten.
         *
         */
        WRITE,
        /**
         *
         * Node was deleted.
         *
         */
        DELETE
    }

    @Override
    PathArgument getIdentifier();

    /**
     * Returns type of modified object.
     *
     * @return type of modified object.
     */
    @Nonnull Class<T> getDataType();

    /**
     *
     * Returns type of modification
     *
     * @return type Type of performed modification.
     */
    @Nonnull ModificationType getModificationType();

    /**
     * Returns after state of top level container.
     *
     * @param root Class representing data container
     * @return State of object after modification. Null if subtree is not present.
     */
    @Nullable T getDataAfter();

    /**
     * Returns unmodifiable collection of modified direct children.
     *
     * @return unmodifiable collection of modified direct children.
     */
    @Nonnull Collection<DataObjectModification<? extends DataObject>> getModifiedChildren();

    /**
     * Returns child modification if {@code child} it was modified by this modification
     * @param child Class of child (only for containers and augmentations)
     * @return Modification of child if was modified, null otherwise.
     */
    @Nullable <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChild(Class<C> child);


    /**
     * Returns child modification if {@code child} it was modified by this modification
     *
     * @param listItem Type of list item
     * @param listKey Type of list key
     * @return  Modification of child if was modified, null otherwise.
     */
    <C extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<C>> DataObjectModification<C> getModifiedChild(
            Class<C> listItem, K listKey);

    /**
     * Returns child modification if {@code child} it was modified by this modification
     * @param child Path Argument of child node
     * @return Modification of child if was modified, null otherwise.
     * @throws IllegalArgumentException If supplied path argument is not valid child of this data object.
     */
    @Nullable DataObjectModification<? extends DataObject> getModifiedChild(PathArgument child);


}
