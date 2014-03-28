package org.opendaylight.controller.md.sal.binding.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;

public class BindingToNormalizedNodeCodec {

    private final BindingIndependentMappingService bindingToLegacy;
    private final DataNormalizer legacyToNormalized;


    public BindingToNormalizedNodeCodec(final BindingIndependentMappingService mappingService,
            final DataNormalizer normalizer) {
        super();
        this.bindingToLegacy = mappingService;
        this.legacyToNormalized = normalizer;
    }

    public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toNormalized(final InstanceIdentifier<? extends DataObject> binding) {
        return legacyToNormalized.toNormalized(bindingToLegacy.toDataDom(binding));
    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier,NormalizedNode<?, ?>> toNormalizedNode(final InstanceIdentifier<? extends DataObject> bindingPath, final DataObject bindingObject) {
        return toNormalizedNode(toEntry(bindingPath,bindingObject));

    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> binding) {
        return legacyToNormalized.toNormalized(bindingToLegacy.toDataDom(binding));
    }

    public InstanceIdentifier<? extends DataObject> toBinding(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized) throws DeserializationException {
        return bindingToLegacy.fromDataDom((legacyToNormalized.toLegacy(normalized)));
    }



    private static final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> ,DataObject > toEntry(final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> key, final DataObject value) {
        return new SimpleEntry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> ,DataObject >(key,value);
    }

    public DataObject toBinding(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> normalizedNode) throws DeserializationException {
        return bindingToLegacy.dataObjectFromDataDom(path, (CompositeNode) DataNormalizer.toLegacy(normalizedNode));
    }

    public DataNormalizer getDataNormalizer() {
        return legacyToNormalized;
    }

    public Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> ,DataObject > toBinding(
            final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized) throws DeserializationException {
        InstanceIdentifier<? extends DataObject> bindingPath = toBinding(normalized.getKey());
        DataObject bindingData = toBinding(bindingPath, normalized.getValue());
        return toEntry(bindingPath, bindingData);
    }




}
