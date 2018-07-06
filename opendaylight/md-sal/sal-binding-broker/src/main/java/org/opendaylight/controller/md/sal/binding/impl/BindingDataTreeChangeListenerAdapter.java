/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * Adapter for translating between {@link DataTreeChangeListener} and
 * {@link org.opendaylight.mdsal.binding.api.DataTreeChangeListener}.
 */
class BindingDataTreeChangeListenerAdapter<T extends DataObject>
        implements org.opendaylight.mdsal.binding.api.DataTreeChangeListener<T> {

    private final DataTreeChangeListener<T> listener;

    BindingDataTreeChangeListenerAdapter(final DataTreeChangeListener<T> listener) {
        this.listener = requireNonNull(listener);
    }

    @Override
    public void onDataTreeChanged(Collection<org.opendaylight.mdsal.binding.api.DataTreeModification<T>> changes) {
        listener.onDataTreeChanged(changes.stream().map(mod -> convert(mod)).collect(Collectors.toList()));
    }

    static <T extends DataObject> DataTreeIdentifier<T> convert(
            org.opendaylight.mdsal.binding.api.DataTreeIdentifier<T> from) {
        return new DataTreeIdentifier<>(convert(from.getDatastoreType()), from.getRootIdentifier());
    }

    static LogicalDatastoreType convert(org.opendaylight.mdsal.common.api.LogicalDatastoreType from) {
        return LogicalDatastoreType.valueOf(from.name());
    }

    private DataTreeModification<T> convert(final org.opendaylight.mdsal.binding.api.DataTreeModification<T> from) {
        final DataTreeIdentifier<T> toRootPath = convert(from.getRootPath());
        return new DataTreeModification<T>() {
            @Override
            public DataTreeIdentifier<T> getRootPath() {
                return toRootPath;
            }

            @Override
            public DataObjectModification<T> getRootNode() {
                return convert(from.getRootNode());
            }
        };
    }

    static <T extends DataObject> DataObjectModification<T> convert(
            final org.opendaylight.mdsal.binding.api.DataObjectModification<T> from) {
        final org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType toModificationType
            = org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType.valueOf(
                    from.getModificationType().name());
        return new DataObjectModification<T>() {
            @Override
            public PathArgument getIdentifier() {
                return from.getIdentifier();
            }

            @Override
            public Class<T> getDataType() {
                return from.getDataType();
            }

            @Override
            public org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType
                    getModificationType() {
                return toModificationType;
            }

            @Override
            public T getDataBefore() {
                return from.getDataBefore();
            }

            @Override
            public T getDataAfter() {
                return from.getDataAfter();
            }

            @Override
            public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
                return from.getModifiedChildren().stream().map(mod -> convert(mod)).collect(Collectors.toList());
            }

            @Override
            public <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(Class<C> child) {
                return convert(from.getModifiedChildContainer(child));
            }

            @Override
            public <C extends Augmentation<T> & DataObject> DataObjectModification<C> getModifiedAugmentation(
                    Class<C> augmentation) {
                return convert(from.getModifiedAugmentation(augmentation));
            }

            @Override
            public <C extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<C>> DataObjectModification<C>
                    getModifiedChildListItem(Class<C> listItem, K listKey) {
                return convert(from.getModifiedChildListItem(listItem, listKey));
            }

            @Override
            public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument childArgument) {
                return convert(from.getModifiedChild(childArgument));
            }
        };
    }

    @Override
    public String toString() {
        return listener.toString();
    }
}
