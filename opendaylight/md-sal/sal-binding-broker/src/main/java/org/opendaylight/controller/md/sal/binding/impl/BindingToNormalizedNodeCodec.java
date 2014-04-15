/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class BindingToNormalizedNodeCodec implements SchemaContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(BindingToNormalizedNodeCodec.class);

    private final BindingIndependentMappingService bindingToLegacy;
    private DataNormalizer legacyToNormalized;

    public BindingToNormalizedNodeCodec(final BindingIndependentMappingService mappingService) {
        super();
        this.bindingToLegacy = mappingService;
    }

    public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toNormalized(
            final InstanceIdentifier<? extends DataObject> binding) {
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier legacyPath = bindingToLegacy.toDataDom(binding);
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = legacyToNormalized.toNormalized(legacyPath);
        LOG.trace("InstanceIdentifier Path {} Serialization: Legacy representation {}, Normalized representation: {}",binding,legacyPath,normalized);
        return normalized;
    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final InstanceIdentifier<? extends DataObject> bindingPath, final DataObject bindingObject) {
        return toNormalizedNode(toEntry(bindingPath, bindingObject));

    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> binding) {
        Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> legacyEntry = bindingToLegacy.toDataDom(binding);
        Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalizedEntry = legacyToNormalized.toNormalized(legacyEntry);
        LOG.trace("Serialization of {}, Legacy Representation: {}, Normalized Representation: {}",binding,legacyEntry,normalizedEntry);
        if(Augmentation.class.isAssignableFrom(binding.getKey().getTargetType())) {

            for(DataContainerChild<? extends PathArgument, ?> child : ((DataContainerNode<?>) normalizedEntry.getValue()).getValue()) {
               if(child instanceof AugmentationNode) {
                   ImmutableList<PathArgument> childArgs = ImmutableList.<PathArgument>builder()
                           .addAll(normalizedEntry.getKey().getPath())
                           .add(child.getIdentifier())
                           .build();
                   org.opendaylight.yangtools.yang.data.api.InstanceIdentifier childPath = new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(childArgs);
                   return new SimpleEntry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>>(childPath,child);
               }
            }

        }
        return normalizedEntry;


    }

    public InstanceIdentifier<? extends DataObject> toBinding(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
            throws DeserializationException {

        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier legacyPath;
        try {
            legacyPath = legacyToNormalized.toLegacy(normalized);
        } catch (DataNormalizationException e) {
            throw new IllegalStateException("Could not denormalize path.",e);
        }
        LOG.trace("InstanceIdentifier Path Deserialization: Legacy representation {}, Normalized representation: {}",legacyPath,normalized);
        return bindingToLegacy.fromDataDom(legacyPath);
    }

    private static final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> toEntry(
            final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> key,
            final DataObject value) {
        return new SimpleEntry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>(
                key, value);
    }

    public DataObject toBinding(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> normalizedNode)
            throws DeserializationException {
        return bindingToLegacy.dataObjectFromDataDom(path, (CompositeNode) DataNormalizer.toLegacy(normalizedNode));
    }

    public DataNormalizer getDataNormalizer() {
        return legacyToNormalized;
    }

    public Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> toBinding(
            final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized)
            throws DeserializationException {
        InstanceIdentifier<? extends DataObject> bindingPath = toBinding(normalized.getKey());
        DataObject bindingData = toBinding(bindingPath, normalized.getValue());
        return toEntry(bindingPath, bindingData);
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext arg0) {
        legacyToNormalized = new DataNormalizer(arg0);
    }

}
