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
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableBiMap;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.binding.data.codec.impl.MissingSchemaException;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BindingToNormalizedNodeCodec implements BindingCodecTreeFactory, BindingNormalizedNodeSerializer, SchemaContextListener, AutoCloseable {

    private static final long WAIT_DURATION_SEC = 5;
    private static final Logger LOG = LoggerFactory.getLogger(BindingToNormalizedNodeCodec.class);

    private final BindingNormalizedNodeCodecRegistry codecRegistry;

    private final GeneratedClassLoadingStrategy classLoadingStrategy;
    private final FutureSchema futureSchema;
    private final LoadingCache<InstanceIdentifier<?>, YangInstanceIdentifier> iiCache = CacheBuilder.newBuilder()
            .softValues().build(new CacheLoader<InstanceIdentifier<?>, YangInstanceIdentifier>() {

                @Override
                public YangInstanceIdentifier load(final InstanceIdentifier<?> key) throws Exception {
                    return toYangInstanceIdentifierBlocking(key);
                }

            });

    private DataNormalizer legacyToNormalized;

    public BindingToNormalizedNodeCodec(final GeneratedClassLoadingStrategy classLoadingStrategy,
            final BindingNormalizedNodeCodecRegistry codecRegistry) {
        this(classLoadingStrategy,codecRegistry,false);

    }

    public BindingToNormalizedNodeCodec(final GeneratedClassLoadingStrategy classLoadingStrategy,
            final BindingNormalizedNodeCodecRegistry codecRegistry,final boolean waitForSchema) {
        this.classLoadingStrategy = Preconditions.checkNotNull(classLoadingStrategy,"classLoadingStrategy");
        this.codecRegistry = Preconditions.checkNotNull(codecRegistry,"codecRegistry");
        this.futureSchema = new FutureSchema(WAIT_DURATION_SEC, TimeUnit.SECONDS, waitForSchema);
    }

    YangInstanceIdentifier toYangInstanceIdentifierBlocking(final InstanceIdentifier<? extends DataObject> binding) {
        try {
            return codecRegistry.toYangInstanceIdentifier(binding);
        } catch (final MissingSchemaException e) {
            waitForSchema(decompose(binding),e);
            return codecRegistry.toYangInstanceIdentifier(binding);
        }
    }

    /**
     * Translates supplied Binding Instance Identifier into NormalizedNode
     * instance identifier.
     *
     * @param binding
     *            Binding Instance Identifier
     * @return DOM Instance Identifier
     * @throws IllegalArgumentException
     *             If supplied Instance Identifier is not valid.
     */
    public YangInstanceIdentifier toNormalized(final InstanceIdentifier<? extends DataObject> binding) {
        return codecRegistry.toYangInstanceIdentifier(binding);
    }

    @Override
    public YangInstanceIdentifier toYangInstanceIdentifier(final InstanceIdentifier<?> binding) {
        return codecRegistry.toYangInstanceIdentifier(binding);
    }


    YangInstanceIdentifier toYangInstanceIdentifierCached(final InstanceIdentifier<?> binding) {
        return iiCache .getUnchecked(binding);
    }

    @Override
    public <T extends DataObject> Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final InstanceIdentifier<T> path, final T data) {
        return codecRegistry.toNormalizedNode(path, data);
    }

    /**
     * Converts Binding Map.Entry to DOM Map.Entry
     *
     * Same as {@link #toNormalizedNode(InstanceIdentifier, DataObject)}.
     *
     * @param binding Map Entry with InstanceIdentifier as key and DataObject as value.
     * @return DOM Map Entry with {@link YangInstanceIdentifier} as key and {@link NormalizedNode}
     *         as value.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding) {
        return toNormalizedNode((InstanceIdentifier) binding.getKey(),binding.getValue());
    }

    @Override
    public Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return codecRegistry.fromNormalizedNode(path, data);
    }

    @Override
    public Notification fromNormalizedNodeNotification(final SchemaPath path, final ContainerNode data) {
        return codecRegistry.fromNormalizedNodeNotification(path, data);
    }

    @Override
    public DataObject fromNormalizedNodeRpcData(final SchemaPath path, final ContainerNode data) {
        return codecRegistry.fromNormalizedNodeRpcData(path, data);
    }

    @Override
    public InstanceIdentifier<?> fromYangInstanceIdentifier(final YangInstanceIdentifier dom) {
        return codecRegistry.fromYangInstanceIdentifier(dom);
    }

    @Override
    public ContainerNode toNormalizedNodeNotification(final Notification data) {
        return codecRegistry.toNormalizedNodeNotification(data);
    }

    @Override
    public ContainerNode toNormalizedNodeRpcData(final DataContainer data) {
        return codecRegistry.toNormalizedNodeRpcData(data);
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
            @Nonnull final Entry<YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized)
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
            @SuppressWarnings("unchecked")
            final Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = Entry.class.cast(codecRegistry.fromNormalizedNode(normalized.getKey(), normalized.getValue()));
            return Optional.fromNullable(binding);
        } catch (final IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        legacyToNormalized = new DataNormalizer(schemaContext);
        BindingRuntimeContext runtimeContext = BindingRuntimeContext.create(classLoadingStrategy, schemaContext);
        codecRegistry.onBindingRuntimeContextUpdated(runtimeContext);
        futureSchema.onRuntimeContextUpdated(runtimeContext);
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
        final Module module = getModuleBlocking(key);
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

    protected ImmutableBiMap<Method, RpcDefinition> getRpcMethodToSchema(final Class<? extends RpcService> key) {
        final Module module = getModuleBlocking(key);
        final ImmutableBiMap.Builder<Method, RpcDefinition> ret = ImmutableBiMap.builder();
        try {
            for (final RpcDefinition rpcDef : module.getRpcs()) {
                final Method method = findRpcMethod(key, rpcDef);
                ret.put(method, rpcDef);
            }
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Rpc defined in model does not have representation in generated class.", e);
        }
        return ret.build();
    }

    private Module getModuleBlocking(final Class<?> modeledClass) {
        final QNameModule moduleName = BindingReflections.getQNameModule(modeledClass);
        final URI namespace = moduleName.getNamespace();
        final Date revision = moduleName.getRevision();
        Module module = runtimeContext().getSchemaContext().findModuleByNamespaceAndRevision(namespace, revision);
        if(module == null && futureSchema.waitForSchema(namespace,revision)) {
            module = runtimeContext().getSchemaContext().findModuleByNamespaceAndRevision(namespace, revision);
        }
        Preconditions.checkState(module != null, "Schema for %s is not available.", modeledClass);
        return module;
    }

    private void waitForSchema(final Collection<Class<?>> binding, final MissingSchemaException e) {
        LOG.warn("Blocking thread to wait for schema convergence updates for {} {}", futureSchema.getDuration(),
                futureSchema.getUnit());
        if(futureSchema.waitForSchema(binding)) {
            return;
        }

        throw e;
    }

    private Method findRpcMethod(final Class<? extends RpcService> key, final RpcDefinition rpcDef) throws NoSuchMethodException {
        final String methodName = BindingMapping.getMethodName(rpcDef.getQName());
        if(rpcDef.getInput() != null) {
            final Class<?> inputClz = runtimeContext().getClassForSchema(rpcDef.getInput());
            return key.getMethod(methodName, inputClz);
        }
        return key.getMethod(methodName);
    }

    private BindingRuntimeContext runtimeContext() {
        return futureSchema.runtimeContext();
    }

    @Override
    public BindingCodecTree create(final BindingRuntimeContext context) {
        return codecRegistry.create(context);
    }

    @Override
    public BindingCodecTree create(final SchemaContext context, final Class<?>... bindingClasses) {
        return codecRegistry.create(context, bindingClasses);
    }

    @Nonnull
    protected Map.Entry<InstanceIdentifier<?>, BindingCodecTreeNode<?>> getSubtreeCodec(
            final YangInstanceIdentifier domIdentifier) {

        final BindingCodecTree currentCodecTree = codecRegistry.getCodecContext();
        final InstanceIdentifier<?> bindingPath = codecRegistry.fromYangInstanceIdentifier(domIdentifier);
        Preconditions.checkArgument(bindingPath != null);
        /**
         * If we are able to deserialize YANG instance identifier, getSubtreeCodec must
         * return non-null value.
         */
        final BindingCodecTreeNode<?> codecContext = currentCodecTree.getSubtreeCodec(bindingPath);
        return new SimpleEntry<InstanceIdentifier<?>, BindingCodecTreeNode<?>>(bindingPath, codecContext);
    }

    @SuppressWarnings("unchecked")
    public Set<Class<? extends Notification>> getNotificationClasses(final Set<SchemaPath> interested) {
        final Set<Class<? extends Notification>> result = new HashSet<>();
        final Set<NotificationDefinition> knownNotifications = runtimeContext().getSchemaContext().getNotifications();
        for (final NotificationDefinition notification : knownNotifications) {
            if (interested.contains(notification.getPath())) {
                try {
                    result.add((Class<? extends Notification>) runtimeContext().getClassForSchema(notification));
                } catch (final IllegalStateException e) {
                    // Ignore
                    LOG.warn("Class for {} is currently not known.",notification.getPath(),e);
                }
            }
        }
        return result;
    }

    private static Collection<Class<?>> decompose(final InstanceIdentifier<?> path) {
        final Set<Class<?>> clazzes = new HashSet<>();
        for(final InstanceIdentifier.PathArgument arg : path.getPathArguments()) {
            clazzes.add(arg.getType());
        }
        return clazzes;
    }

}
