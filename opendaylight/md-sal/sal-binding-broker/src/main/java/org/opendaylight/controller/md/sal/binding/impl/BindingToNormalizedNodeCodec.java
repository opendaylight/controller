/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingToNormalizedNodeCodec implements SchemaContextListener,AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BindingToNormalizedNodeCodec.class);

    private final BindingIndependentMappingService bindingToLegacy;
    private final BindingNormalizedNodeCodecRegistry codecRegistry;
    private DataNormalizer legacyToNormalized;
    private final GeneratedClassLoadingStrategy classLoadingStrategy;

    public BindingToNormalizedNodeCodec(final GeneratedClassLoadingStrategy classLoadingStrategy, final BindingIndependentMappingService mappingService, final BindingNormalizedNodeCodecRegistry codecRegistry) {
        super();
        this.bindingToLegacy = mappingService;
        this.classLoadingStrategy = classLoadingStrategy;
        this.codecRegistry = codecRegistry;

    }

    public org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier toNormalized(
            final InstanceIdentifier<? extends DataObject> binding) {
        return codecRegistry.toYangInstanceIdentifier(binding);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Entry<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final InstanceIdentifier<? extends DataObject> bindingPath, final DataObject bindingObject) {
        return codecRegistry.toNormalizedNode((InstanceIdentifier) bindingPath, bindingObject);

    }

    public Entry<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> binding) {
        return toNormalizedNode(binding.getKey(),binding.getValue());
    }

    /**
     *
     * Returns a Binding-Aware instance identifier from normalized
     * instance-identifier if it is possible to create representation.
     *
     * Returns Optional.absent for cases where target is mixin node except
     * augmentation.
     *
     */
    public Optional<InstanceIdentifier<? extends DataObject>> toBinding(
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier normalized)
                    throws DeserializationException {
        try {
            return  Optional.<InstanceIdentifier<? extends DataObject>>of(codecRegistry.fromYangInstanceIdentifier(normalized));
        } catch (IllegalArgumentException e) {
            return Optional.absent();
        }
    }


    private static final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> toBindingEntry(
            final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> key,
            final DataObject value) {
        return new SimpleEntry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>(
                key, value);
    }

    public DataNormalizer getDataNormalizer() {
        return legacyToNormalized;
    }

    @SuppressWarnings("unchecked")
    public Optional<Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>> toBinding(
            final Entry<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized)
                    throws DeserializationException {
        try {
            @SuppressWarnings("rawtypes")
            Entry binding = codecRegistry.fromNormalizedNode(normalized.getKey(), normalized.getValue());
            return Optional.<Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>>fromNullable(binding);
        } catch (IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext arg0) {
        legacyToNormalized = new DataNormalizer(arg0);
        codecRegistry.onBindingRuntimeContextUpdated(BindingRuntimeContext.create(classLoadingStrategy, arg0));
    }

    public <T extends DataObject> Function<Optional<NormalizedNode<?, ?>>, Optional<T>>  deserializeFunction(final InstanceIdentifier<T> path) {
        return codecRegistry.deserializeFunction(path);
    }

    /**
     * Returns an default object according to YANG schema for supplied path.
     *
     * @param path DOM Path
     * @return Node with defaults set on.
     */
    public NormalizedNode<?, ?> getDefaultNodeFor(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path) {
        Iterator<PathArgument> iterator = path.getPathArguments().iterator();
        DataNormalizationOperation<?> currentOp = legacyToNormalized.getRootOperation();
        while (iterator.hasNext()) {
            PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
        }
        return currentOp.createDefault(path.getLastPathArgument());
    }

    public BindingIndependentMappingService getLegacy() {
        return bindingToLegacy;
    }

    @Override
    public void close() throws Exception {
        // NOOP Intentionally
    }
}
