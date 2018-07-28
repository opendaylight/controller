/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.collect.Collections2;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * Represents a modification of DataObject.
 */
public interface DataObjectModification<T extends DataObject>
        extends org.opendaylight.yangtools.concepts.Identifiable<PathArgument> {

    enum ModificationType {
        /**
         * Child node (direct or indirect) was modified.
         *
         */
        SUBTREE_MODIFIED,

        /**
         * Node was explicitly created / overwritten.
         *
         */

        WRITE,
        /**
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
     * Returns type of modification.
     *
     * @return type Type of performed modification.
     */
    @Nonnull ModificationType getModificationType();

    /**
     * Returns before-state of top level container. Implementations are encouraged,
     * but not required to provide this state.
     *
     * @return State of object before modification. Null if subtree was not present,
     *         or the implementation cannot provide the state.
     */
    @Nullable T getDataBefore();

    /**
     * Returns after-state of top level container.
     *
     * @return State of object after modification. Null if subtree is not present.
     */
    @Nullable T getDataAfter();

    /**
     * Returns unmodifiable collection of modified direct children.
     *
     * @return unmodifiable collection of modified direct children.
     */
    @Nonnull Collection<? extends DataObjectModification<? extends DataObject>> getModifiedChildren();

    /**
     * Returns child list item modification if {@code child} was modified by this modification. This method should be
     * used if the child is defined in a grouping brought into a case inside this object.
     *
     * @param caseType Case type class
     * @param childType Type of list item - must be list item with key
     * @return Modification of {@code child} if {@code child} was modified, null otherwise.
     * @throws IllegalArgumentException If supplied {@code childType} class is not valid child according
     *         to generated model.
     */
    default <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>>
        Collection<DataObjectModification<C>> getModifiedChildren(@Nonnull final Class<H> caseType,
                @Nonnull final Class<C> childType) {
        final Item<C> item = Item.of(caseType, childType);
        return (Collection<DataObjectModification<C>>) Collections2.filter(getModifiedChildren(),
            mod -> item.equals(mod.getIdentifier()));
    }

    /**
     * Returns container child modification if {@code child} was modified by this modification. This method should be
     * used if the child is defined in a grouping brought into a case inside this object.
     *
     * <p>
     * For accessing all modified list items consider iterating over {@link #getModifiedChildren()}.
     *
     * @param caseType Case type class
     * @param child Type of child - must be only container
     * @return Modification of {@code child} if {@code child} was modified, null otherwise.
     * @throws IllegalArgumentException If supplied {@code child} class is not valid child according
     *         to generated model.
     */
    default @Nullable <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>>
            DataObjectModification<C> getModifiedChildContainer(@Nonnull final Class<H> caseType,
                    @Nonnull final Class<C> child) {
        return (DataObjectModification<C>) getModifiedChild(Item.of(caseType, child));
    }

    /**
     * Returns container child modification if {@code child} was modified by this modification.
     *
     * <p>
     * For accessing all modified list items consider iterating over {@link #getModifiedChildren()}.
     *
     * @param child Type of child - must be only container
     * @return Modification of {@code child} if {@code child} was modified, null otherwise.
     * @throws IllegalArgumentException If supplied {@code child} class is not valid child according
     *         to generated model.
     */
    @Nullable <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(
            @Nonnull Class<C> child);

    /**
     * Returns augmentation child modification if {@code augmentation} was modified by this modification.
     *
     * <p>
     * For accessing all modified list items consider iterating over {@link #getModifiedChildren()}.
     *
     * @param augmentation Type of augmentation - must be only container
     * @return Modification of {@code augmentation} if {@code augmentation} was modified, null otherwise.
     * @throws IllegalArgumentException If supplied {@code augmentation} class is not valid augmentation
     *         according to generated model.
     */
    @Nullable <C extends Augmentation<T> & DataObject> DataObjectModification<C> getModifiedAugmentation(
            @Nonnull Class<C> augmentation);

    /**
     * Returns child list item modification if {@code child} was modified by this modification.
     *
     * @param listItem Type of list item - must be list item with key
     * @param listKey List item key
     * @return Modification of {@code child} if {@code child} was modified, null otherwise.
     * @throws IllegalArgumentException If supplied {@code listItem} class is not valid child according
     *         to generated model.
     */
    <N extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<N>> DataObjectModification<N>
            getModifiedChildListItem(@Nonnull Class<N> listItem, @Nonnull K listKey);

    /**
     * Returns child list item modification if {@code child} was modified by this modification.
     *
     * @param caseType Case type class
     * @param listItem Type of list item - must be list item with key
     * @param listKey List item key
     * @return Modification of {@code child} if {@code child} was modified, null otherwise.
     * @throws IllegalArgumentException If supplied {@code listItem} class is not valid child according
     *         to generated model.
     */
    default <H extends ChoiceIn<? super T> & DataObject, C extends Identifiable<K> & ChildOf<? super H>,
            K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    @Nonnull final Class<H> caseType, @Nonnull final Class<C> listItem, @Nonnull final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(IdentifiableItem.of(caseType, listItem, listKey));
    }

    /**
     * Returns a child modification if a node identified by {@code childArgument} was modified by
     * this modification.
     *
     * @param childArgument Path Argument of child node
     * @return Modification of child identified by {@code childArgument} if {@code childArgument}
     *         was modified, null otherwise.
     * @throws IllegalArgumentException If supplied path argument is not valid child according to
     *         generated model.
     *
     */
    @Nullable DataObjectModification<? extends DataObject> getModifiedChild(PathArgument childArgument);
}
