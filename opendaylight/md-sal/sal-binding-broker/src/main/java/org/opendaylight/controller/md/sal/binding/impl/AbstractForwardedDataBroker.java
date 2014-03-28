package org.opendaylight.controller.md.sal.binding.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.xtext.xbase.lib.Exceptions;
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

public abstract class AbstractForwardedDataBroker implements Delegator<DOMDataBroker>, DomForwardedBroker, SchemaContextListener {

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
        ListenerRegistration<DOMDataChangeListener> domRegistration = domDataBroker.registerDataChangeListener(store, domPath, domDataChangeListener, triggeringScope);
        return new ListenerRegistrationImpl(listener, domRegistration);
    }

    protected Map<InstanceIdentifier<?>, DataObject> fromDOMToData(
            final Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized) {
        Map<InstanceIdentifier<?>, DataObject> newMap = new HashMap<>();
        for (Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : normalized
                .entrySet()) {
            try {
                Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = getCodec().toBinding(entry);
                newMap.put(binding.getKey(), binding.getValue());
            } catch (DeserializationException e) {
                LOG.debug("Ommiting {}",entry,e);
            }
        }
        return newMap;
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
            bindingDataChangeListener.onDataChanged(new TranslatedDataChangeEvent(change,path));
        }
    }

    private class TranslatedDataChangeEvent implements AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {
        private final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> domEvent;
        private InstanceIdentifier<?> path;

        public TranslatedDataChangeEvent(
                final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change) {
            this.domEvent = change;
        }

        public TranslatedDataChangeEvent(
                final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change,
                final InstanceIdentifier<?> path) {
            this.domEvent = change;
            this.path = path;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
            return fromDOMToData(domEvent.getCreatedData());
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
            return fromDOMToData(domEvent.getUpdatedData());

        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedPaths() {
            final Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> removedPaths = domEvent
                    .getRemovedPaths();
            final Set<InstanceIdentifier<?>> output = new HashSet<>();
            for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier : removedPaths) {
                try {
                    output.add(mappingService.fromDataDom(instanceIdentifier));
                } catch (DeserializationException e) {
                    Exceptions.sneakyThrow(e);
                }
            }

            return output;
        }

        @Override
        public Map<InstanceIdentifier<?>, ? extends DataObject> getOriginalData() {
            return fromDOMToData(domEvent.getOriginalData());

        }

        @Override
        public DataObject getOriginalSubtree() {

            return toBindingData(path,domEvent.getOriginalSubtree());
        }

        @Override
        public DataObject getUpdatedSubtree() {

            return toBindingData(path,domEvent.getUpdatedSubtree());
        }

        @Override
        public String toString() {
            return "TranslatedDataChangeEvent [domEvent=" + domEvent + "]";
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

    protected DataObject toBindingData(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> data) {
        try {
            return getCodec().toBinding(path, data);
        } catch (DeserializationException e) {
            return null;
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
