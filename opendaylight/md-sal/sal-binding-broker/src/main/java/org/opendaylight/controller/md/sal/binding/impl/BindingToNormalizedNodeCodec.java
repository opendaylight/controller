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
import com.google.common.collect.ImmutableBiMap;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BindingToNormalizedNodeCodec implements SchemaContextListener,AutoCloseable {

    private final BindingIndependentMappingService bindingToLegacy;
    private final BindingNormalizedNodeCodecRegistry codecRegistry;
    private DataNormalizer legacyToNormalized;
    private final GeneratedClassLoadingStrategy classLoadingStrategy;
    private BindingRuntimeContext runtimeContext;

    public BindingToNormalizedNodeCodec(final GeneratedClassLoadingStrategy classLoadingStrategy, final BindingIndependentMappingService mappingService, final BindingNormalizedNodeCodecRegistry codecRegistry) {
        super();
        this.bindingToLegacy = mappingService;
        this.classLoadingStrategy = classLoadingStrategy;
        this.codecRegistry = codecRegistry;

    }

    public YangInstanceIdentifier toNormalized(final InstanceIdentifier<? extends DataObject> binding) {
        return codecRegistry.toYangInstanceIdentifier(binding);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final InstanceIdentifier<? extends DataObject> bindingPath, final DataObject bindingObject) {
        return codecRegistry.toNormalizedNode((InstanceIdentifier) bindingPath, bindingObject);

    }

    public Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding) {
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
    public Optional<InstanceIdentifier<? extends DataObject>> toBinding(final YangInstanceIdentifier normalized)
                    throws DeserializationException {
        try {
            return Optional.<InstanceIdentifier<? extends DataObject>>fromNullable(codecRegistry.fromYangInstanceIdentifier(normalized));
        } catch (final IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    public DataNormalizer getDataNormalizer() {
        return legacyToNormalized;
    }

    public Optional<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> toBinding(
            final @Nonnull Entry<YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized)
                    throws DeserializationException {
        try {
            /*
             * This cast is required, due to generics behaviour in openjdk / oracle javac
             *
             * InstanceIdentifier has definition InstanceIdentifier<T extends DataObject>,
             * this means '?' is always  <? extends DataObject>. Eclipse compiler
             * is able to determine this relationship and treats
             * Entry<InstanceIdentifier<?>,DataObject> and Entry<InstanceIdentifier<? extends DataObject,DataObject>
             * as assignable. However openjdk / oracle javac treats this two types
             * as incompatible and issues a compile error.
             *
             * It is safe to  loose generic information and cast it to other generic signature.
             *
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = Entry.class.cast(codecRegistry.fromNormalizedNode(normalized.getKey(), normalized.getValue()));
            return Optional.fromNullable(binding);
        } catch (final IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext arg0) {
        legacyToNormalized = new DataNormalizer (arg0);
        runtimeContext = BindingRuntimeContext.create(classLoadingStrategy, arg0);
        codecRegistry.onBindingRuntimeContextUpdated(runtimeContext);
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
    public NormalizedNode<?, ?> getDefaultNodeFor(final YangInstanceIdentifier path) {
        final Iterator<PathArgument> iterator = path.getPathArguments().iterator();
        DataNormalizationOperation<?> currentOp = legacyToNormalized.getRootOperation();
        while (iterator.hasNext()) {
            final PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (final DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
        }
        return currentOp.createDefault(path.getLastPathArgument());
    }

    public BindingIndependentMappingService getLegacy() {
        return bindingToLegacy;
    }

    public BindingNormalizedNodeCodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    @Override
    public void close() {
        // NOOP Intentionally
    }

    public BindingNormalizedNodeCodecRegistry getCodecFactory() {
        return codecRegistry;
    }

    // FIXME: This should be probably part of Binding Runtime context
    public ImmutableBiMap<Method, SchemaPath> getRpcMethodToSchemaPath(final Class<? extends RpcService> key) {
        final QNameModule moduleName = BindingReflections.getQNameModule(key);
        final Module module = runtimeContext.getSchemaContext().findModuleByNamespaceAndRevision(moduleName.getNamespace(), moduleName.getRevision());
        final ImmutableBiMap.Builder<Method, SchemaPath> ret = ImmutableBiMap.<Method, SchemaPath>builder();
        try {
            for (final RpcDefinition rpcDef : module.getRpcs()) {
                final Method method = findRpcMethod(key, rpcDef);
                ret.put(method, rpcDef.getPath());
            }
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Rpc defined in model does not have representation in generated class.", e);
        }
        return ret.build();
    }

    private Method findRpcMethod(final Class<? extends RpcService> key, final RpcDefinition rpcDef) throws NoSuchMethodException {
        final String methodName = BindingMapping.getMethodName(rpcDef.getQName());
        if(rpcDef.getInput() != null) {
            final Class<?> inputClz = runtimeContext.getClassForSchema(rpcDef.getInput());
            return key.getMethod(methodName, inputClz);
        }
        return key.getMethod(methodName);
    }
}
