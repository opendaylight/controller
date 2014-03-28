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
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public abstract class AbstractForwardedDataBroker implements Delegator<DOMDataBroker>, SchemaContextListener {


    // The Broker to whom we do all forwarding
    private final DOMDataBroker domDataBroker;

    // Mapper to convert from Binding Independent objects to Binding Aware
    // objects
    private final BindingIndependentMappingService mappingService;



    private BindingToNormalizedNodeCodec codec;

    protected AbstractForwardedDataBroker(final DOMDataBroker domDataBroker, final BindingIndependentMappingService mappingService) {
        this.domDataBroker = domDataBroker;
        this.mappingService = mappingService;
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
        codec = new BindingToNormalizedNodeCodec(mappingService, new DataNormalizer(ctx));
    }

    public ListenerRegistration<BindingDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final BindingDataChangeListener listener, final DataChangeScope triggeringScope) {
        DOMDataChangeListener domDataChangeListener = new TranslatingDataChangeInvoker(store, path, listener, triggeringScope);
        return new ListenerRegistrationImpl(listener, domDataBroker.registerDataChangeListener(store, mappingService.toDataDom(path), domDataChangeListener, triggeringScope));
    }


    protected Map<InstanceIdentifier<?>, DataObject> fromDOMToData(
            final Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> createdData) {
        Map<InstanceIdentifier<?>, DataObject> newMap = new HashMap<>();
        for (Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : createdData
                .entrySet()) {
            try {
                Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = getCodec().toBinding(entry);
                newMap.put(binding.getKey(), binding.getValue());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
        }
        return newMap;
    }


    private class TranslatingDataChangeInvoker implements DOMDataChangeListener {
        private final BindingDataChangeListener bindingDataChangeListener;
        private final LogicalDatastoreType store;
        private final InstanceIdentifier<?> path;
        private final DataChangeScope triggeringScope;

        public TranslatingDataChangeInvoker(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final BindingDataChangeListener bindingDataChangeListener, final DataChangeScope triggeringScope){
            this.store = store;
            this.path = path;
            this.bindingDataChangeListener = bindingDataChangeListener;
            this.triggeringScope = triggeringScope;
        }

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change) {
            bindingDataChangeListener.onDataChanged(new TranslatedDataChangeEventImpl(change));
        }
    }


    private class TranslatedDataChangeEventImpl implements AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {
        private final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> dataChangeEvent;

        public TranslatedDataChangeEventImpl(final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change){
            this.dataChangeEvent = change;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
            return fromDOMToData(dataChangeEvent.getCreatedData());
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
            return fromDOMToData(dataChangeEvent.getUpdatedData());

        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedPaths() {
            final Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> removedPaths = dataChangeEvent.getRemovedPaths();
            final Set<InstanceIdentifier<?>> output = new HashSet<>();
            for(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier : removedPaths){
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
            return fromDOMToData(dataChangeEvent.getOriginalData());

        }

        @Override
        public DataObject getOriginalSubtree() {
            try {
                return mappingService.dataObjectFromDataDom(InstanceIdentifier.builder().build(), (CompositeNode) dataChangeEvent.getOriginalSubtree());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }

        @Override
        public DataObject getUpdatedSubtree() {
            try {
                return mappingService.dataObjectFromDataDom(InstanceIdentifier.builder().build(), (CompositeNode) dataChangeEvent.getUpdatedSubtree());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }
    }


    private static class ListenerRegistrationImpl extends AbstractListenerRegistration<BindingDataChangeListener> {
        private final ListenerRegistration<DOMDataChangeListener> registration;
        public ListenerRegistrationImpl(final BindingDataChangeListener listener, final ListenerRegistration<DOMDataChangeListener> registration) {
            super(listener);
            this.registration = registration;
        }

        @Override
        protected void removeRegistration() {
            registration.close();
        }
    }


}
