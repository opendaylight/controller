/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.binding.impl.forward.DomForwardedBroker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

public abstract class AbstractForwardedDataBroker implements Delegator<DOMDataBroker>, DomForwardedBroker,
        SchemaContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractForwardedDataBroker.class);
    // The Broker to whom we do all forwarding
    private final DOMDataBroker domDataBroker;

    // Mapper to convert from Binding Independent objects to Binding Aware
    // objects
    private final BindingIndependentMappingService mappingService;

    private final BindingToNormalizedNodeCodec codec;
    private BindingIndependentConnector connector;
    private ProviderSession context;

    protected AbstractForwardedDataBroker(final DOMDataBroker domDataBroker,
            final BindingIndependentMappingService mappingService) {
        this.domDataBroker = domDataBroker;
        this.mappingService = mappingService;
        this.codec = new BindingToNormalizedNodeCodec(mappingService);
    }

    protected BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    protected BindingIndependentMappingService getMappingService() {
        return mappingService;
    }

    @Override
    public DOMDataBroker getDelegate() {
        return domDataBroker;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext ctx) {
        codec.onGlobalContextUpdated(ctx);
    }

    public ListenerRegistration<BindingDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final BindingDataChangeListener listener,
            final DataChangeScope triggeringScope) {
        DOMDataChangeListener domDataChangeListener = new TranslatingDataChangeInvoker(store, path, listener,
                triggeringScope);
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier domPath = codec.toNormalized(path);
        ListenerRegistration<DOMDataChangeListener> domRegistration = domDataBroker.registerDataChangeListener(store,
                domPath, domDataChangeListener, triggeringScope);
        return new ListenerRegistrationImpl(listener, domRegistration);
    }

    protected Map<InstanceIdentifier<?>, DataObject> toBinding(
            final Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized) {
        Map<InstanceIdentifier<?>, DataObject> newMap = new HashMap<>();
        for (Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : normalized
                .entrySet()) {
            try {
                Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = getCodec().toBinding(entry);
                newMap.put(binding.getKey(), binding.getValue());
            } catch (DeserializationException e) {
                LOG.debug("Omitting {}", entry, e);
            }
        }
        return newMap;
    }

    protected Set<InstanceIdentifier<?>> toBinding(
            final Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> normalized) {
        Set<InstanceIdentifier<?>> hashSet = new HashSet<>();
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath : normalized) {
            try {
                InstanceIdentifier<? extends DataObject> binding = getCodec().toBinding(normalizedPath);
                hashSet.add(binding);
            } catch (DeserializationException e) {
                LOG.debug("Omitting {}", normalizedPath, e);
            }
        }
        return hashSet;
    }

    protected Optional<DataObject> toBindingData(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> data) {
        if(path.isWildcarded()) {
            return Optional.absent();
        }

        try {
            return Optional.fromNullable(getCodec().toBinding(path, data));
        } catch (DeserializationException e) {
            return Optional.absent();
        }
    }

    private class TranslatingDataChangeInvoker implements DOMDataChangeListener {
        private final BindingDataChangeListener bindingDataChangeListener;
        private final LogicalDatastoreType store;
        private final InstanceIdentifier<?> path;
        private final DataChangeScope triggeringScope;

        public TranslatingDataChangeInvoker(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
                final BindingDataChangeListener bindingDataChangeListener, final DataChangeScope triggeringScope) {
            this.store = store;
            this.path = path;
            this.bindingDataChangeListener = bindingDataChangeListener;
            this.triggeringScope = triggeringScope;
        }

        @Override
        public void onDataChanged(
                final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change) {
            bindingDataChangeListener.onDataChanged(new TranslatedDataChangeEvent(change, path));
        }
    }

    private class TranslatedDataChangeEvent implements AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {
        private final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> domEvent;
        private final InstanceIdentifier<?> path;

        private Map<InstanceIdentifier<?>, DataObject> createdCache;
        private Map<InstanceIdentifier<?>, DataObject> updatedCache;
        private Map<InstanceIdentifier<?>, ? extends DataObject> originalCache;
        private Set<InstanceIdentifier<?>> removedCache;
        private Optional<DataObject> originalDataCache;
        private Optional<DataObject> updatedDataCache;

        public TranslatedDataChangeEvent(
                final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change,
                final InstanceIdentifier<?> path) {
            this.domEvent = change;
            this.path = path;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
            if (createdCache == null) {
                createdCache = Collections.unmodifiableMap(toBinding(domEvent.getCreatedData()));
            }
            return createdCache;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
            if (updatedCache == null) {
                updatedCache = Collections.unmodifiableMap(toBinding(domEvent.getUpdatedData()));
            }
            return updatedCache;

        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedPaths() {
            if (removedCache == null) {
                removedCache = Collections.unmodifiableSet(toBinding(domEvent.getRemovedPaths()));
            }
            return removedCache;
        }

        @Override
        public Map<InstanceIdentifier<?>, ? extends DataObject> getOriginalData() {
            if (originalCache == null) {
                originalCache = Collections.unmodifiableMap(toBinding(domEvent.getOriginalData()));
            }
            return originalCache;

        }

        @Override
        public DataObject getOriginalSubtree() {
            if (originalDataCache == null) {
                originalDataCache = toBindingData(path, domEvent.getOriginalSubtree());
            }
            return originalDataCache.orNull();
        }

        @Override
        public DataObject getUpdatedSubtree() {
            if (updatedDataCache == null) {
                updatedDataCache = toBindingData(path, domEvent.getUpdatedSubtree());
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

    private static class ListenerRegistrationImpl extends AbstractListenerRegistration<BindingDataChangeListener> {
        private final ListenerRegistration<DOMDataChangeListener> registration;

        public ListenerRegistrationImpl(final BindingDataChangeListener listener,
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
    public BindingIndependentConnector getConnector() {
        return this.connector;
    }

    @Override
    public ProviderSession getDomProviderContext() {
        return this.context;
    }

    @Override
    public void setConnector(final BindingIndependentConnector connector) {
        this.connector = connector;
    }

    @Override
    public void setDomProviderContext(final ProviderSession domProviderContext) {
        this.context = domProviderContext;
    }

    @Override
    public void startForwarding() {
        // NOOP
    }

}
