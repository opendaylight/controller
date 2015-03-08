/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractForwardedDataBroker implements Delegator<DOMDataBroker>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractForwardedDataBroker.class);
    // The Broker to whom we do all forwarding
    private final DOMDataBroker domDataBroker;

    private final BindingToNormalizedNodeCodec codec;

    protected AbstractForwardedDataBroker(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec,
            final SchemaService schemaService) {
        this.domDataBroker = domDataBroker;
        this.codec = codec;
    }

    protected AbstractForwardedDataBroker(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec) {
        this.domDataBroker = domDataBroker;
        this.codec = codec;
    }

    protected BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    @Override
    public DOMDataBroker getDelegate() {
        return domDataBroker;
    }

    public ListenerRegistration<DataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataChangeListener listener, final DataChangeScope triggeringScope) {
        final DOMDataChangeListener domDataChangeListener = new TranslatingDataChangeInvoker(store, path, listener,
                triggeringScope);
        final YangInstanceIdentifier domPath = codec.toNormalized(path);
        final ListenerRegistration<DOMDataChangeListener> domRegistration = domDataBroker.registerDataChangeListener(store,
                domPath, domDataChangeListener, triggeringScope);
        return new ListenerRegistrationImpl(listener, domRegistration);
    }

    protected Map<InstanceIdentifier<?>, DataObject> toBinding(final InstanceIdentifier<?> path,
            final Map<YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized) {
        final Map<InstanceIdentifier<?>, DataObject> newMap = new HashMap<>();

        for (final Map.Entry<YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : normalized.entrySet()) {
            try {
                final Optional<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> potential = getCodec().toBinding(entry);
                if (potential.isPresent()) {
                    final Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = potential.get();
                    newMap.put(binding.getKey(), binding.getValue());
                }
            } catch (final DeserializationException e) {
                LOG.warn("Failed to transform {}, omitting it", entry, e);
            }
        }
        return newMap;
    }

    protected Set<InstanceIdentifier<?>> toBinding(final InstanceIdentifier<?> path,
            final Set<YangInstanceIdentifier> normalized) {
        final Set<InstanceIdentifier<?>> hashSet = new HashSet<>();
        for (final YangInstanceIdentifier normalizedPath : normalized) {
            try {
                final Optional<InstanceIdentifier<? extends DataObject>> potential = getCodec().toBinding(normalizedPath);
                if (potential.isPresent()) {
                    final InstanceIdentifier<? extends DataObject> binding = potential.get();
                    hashSet.add(binding);
                } else if (normalizedPath.getLastPathArgument() instanceof YangInstanceIdentifier.AugmentationIdentifier) {
                    hashSet.add(path);
                }
            } catch (final DeserializationException e) {
                LOG.warn("Failed to transform {}, omitting it", normalizedPath, e);
            }
        }
        return hashSet;
    }

    protected Optional<DataObject> toBindingData(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> data) {
        if (path.isWildcarded()) {
            return Optional.absent();
        }
        return (Optional<DataObject>) getCodec().deserializeFunction(path).apply(Optional.<NormalizedNode<?, ?>> of(data));
    }

    private class TranslatingDataChangeInvoker implements DOMDataChangeListener {
        private final DataChangeListener bindingDataChangeListener;
        private final LogicalDatastoreType store;
        private final InstanceIdentifier<?> path;
        private final DataChangeScope triggeringScope;

        public TranslatingDataChangeInvoker(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
                final DataChangeListener bindingDataChangeListener, final DataChangeScope triggeringScope) {
            this.store = store;
            this.path = path;
            this.bindingDataChangeListener = bindingDataChangeListener;
            this.triggeringScope = triggeringScope;
        }

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
            bindingDataChangeListener.onDataChanged(new TranslatedDataChangeEvent(change, path));
        }

        @Override
        public String toString() {
            return bindingDataChangeListener.getClass().getName();
        }
    }

    private class TranslatedDataChangeEvent implements AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {
        private final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> domEvent;
        private final InstanceIdentifier<?> path;

        private Map<InstanceIdentifier<?>, DataObject> createdCache;
        private Map<InstanceIdentifier<?>, DataObject> updatedCache;
        private Map<InstanceIdentifier<?>, DataObject> originalCache;
        private Set<InstanceIdentifier<?>> removedCache;
        private Optional<DataObject> originalDataCache;
        private Optional<DataObject> updatedDataCache;

        public TranslatedDataChangeEvent(
                final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change,
                final InstanceIdentifier<?> path) {
            this.domEvent = change;
            this.path = path;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
            if (createdCache == null) {
                createdCache = Collections.unmodifiableMap(toBinding(path, domEvent.getCreatedData()));
            }
            return createdCache;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
            if (updatedCache == null) {
                updatedCache = Collections.unmodifiableMap(toBinding(path, domEvent.getUpdatedData()));
            }
            return updatedCache;

        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedPaths() {
            if (removedCache == null) {
                removedCache = Collections.unmodifiableSet(toBinding(path, domEvent.getRemovedPaths()));
            }
            return removedCache;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getOriginalData() {
            if (originalCache == null) {
                originalCache = Collections.unmodifiableMap(toBinding(path, domEvent.getOriginalData()));
            }
            return originalCache;

        }

        @Override
        public DataObject getOriginalSubtree() {
            if (originalDataCache == null) {
                if (domEvent.getOriginalSubtree() != null) {
                    originalDataCache = toBindingData(path, domEvent.getOriginalSubtree());
                } else {
                    originalDataCache = Optional.absent();
                }
            }
            return originalDataCache.orNull();
        }

        @Override
        public DataObject getUpdatedSubtree() {
            if (updatedDataCache == null) {
                if (domEvent.getUpdatedSubtree() != null) {
                    updatedDataCache = toBindingData(path, domEvent.getUpdatedSubtree());
                } else {
                    updatedDataCache = Optional.absent();
                }
            }
            return updatedDataCache.orNull();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(TranslatedDataChangeEvent.class) //
                    .add("created", getCreatedData()) //
                    .add("updated", getUpdatedData()) //
                    .add("removed", getRemovedPaths()) //
                    .add("dom", domEvent) //
                    .toString();
        }
    }

    private static class ListenerRegistrationImpl extends AbstractListenerRegistration<DataChangeListener> {
        private final ListenerRegistration<DOMDataChangeListener> registration;

        public ListenerRegistrationImpl(final DataChangeListener listener,
                final ListenerRegistration<DOMDataChangeListener> registration) {
            super(listener);
            this.registration = registration;
        }

        @Override
        protected void removeRegistration() {
            registration.close();
        }
    }

    @Override
    public void close() {
    }

}
