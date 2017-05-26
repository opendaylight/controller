/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeService;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;


/**
 *
 * Adapter exposing Binding {@link DataTreeChangeService} and wrapping
 * {@link DOMDataTreeChangeService} and is responsible for translation
 * and instantiation of {@link BindingDOMDataTreeChangeListenerAdapter}
 * adapters.
 *
 * Each registered {@link DataTreeChangeListener} is wrapped using
 * adapter and registered directly to DOM service.
 */
final class BindingDOMDataTreeChangeServiceAdapter implements DataTreeChangeService {

    private final BindingToNormalizedNodeCodec codec;
    private final DOMDataTreeChangeService dataTreeChangeService;

    private BindingDOMDataTreeChangeServiceAdapter(final BindingToNormalizedNodeCodec codec,
            final DOMDataTreeChangeService dataTreeChangeService) {
        this.codec = Preconditions.checkNotNull(codec);
        this.dataTreeChangeService = Preconditions.checkNotNull(dataTreeChangeService);
    }

    static DataTreeChangeService create(final BindingToNormalizedNodeCodec codec,
            final DOMDataTreeChangeService dataTreeChangeService) {
        return new BindingDOMDataTreeChangeServiceAdapter(codec, dataTreeChangeService);
    }

    @Override
    public <T extends DataObject, L extends DataTreeChangeListener<T>> ListenerRegistration<L> registerDataTreeChangeListener(
            final DataTreeIdentifier<T> treeId, final L listener) {
        final DOMDataTreeIdentifier domIdentifier = toDomTreeIdentifier(treeId);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final BindingDOMDataTreeChangeListenerAdapter<T> domListener =
                listener instanceof ClusteredDataTreeChangeListener ?
                        new BindingClusteredDOMDataTreeChangeListenerAdapter<>(
                                codec, (ClusteredDataTreeChangeListener) listener, treeId.getDatastoreType()) :
                        new BindingDOMDataTreeChangeListenerAdapter<>(codec, listener, treeId.getDatastoreType());

        final ListenerRegistration<BindingDOMDataTreeChangeListenerAdapter<T>> domReg =
                dataTreeChangeService.registerDataTreeChangeListener(domIdentifier, domListener);
        return new BindingDataTreeChangeListenerRegistration<>(listener,domReg);
    }

    private DOMDataTreeIdentifier toDomTreeIdentifier(final DataTreeIdentifier<?> treeId) {
        final YangInstanceIdentifier domPath = codec.toYangInstanceIdentifierBlocking(treeId.getRootIdentifier());
        return new DOMDataTreeIdentifier(treeId.getDatastoreType(), domPath);
    }
}
