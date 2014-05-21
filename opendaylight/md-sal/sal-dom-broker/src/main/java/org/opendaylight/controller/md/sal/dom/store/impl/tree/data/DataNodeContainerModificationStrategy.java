/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.AugmentationSchemaProxy;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

abstract class DataNodeContainerModificationStrategy<T extends DataNodeContainer> extends NormalizedNodeContainerModificationStrategy {

    private final T schema;
    private final LoadingCache<PathArgument, ModificationApplyOperation> childCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from(new Function<PathArgument, ModificationApplyOperation>() {

                @Override
                public ModificationApplyOperation apply(final PathArgument identifier) {
                    if (identifier instanceof AugmentationIdentifier && schema instanceof AugmentationTarget) {
                        return from(schema, (AugmentationTarget) schema, (AugmentationIdentifier) identifier);
                    }

                    DataSchemaNode child = schema.getDataChildByName(identifier.getNodeType());
                    if (child == null) {
                        return null;
                    }
                    return from(child);
                }
            }));

    protected DataNodeContainerModificationStrategy(final T schema,
            final Class<? extends NormalizedNode<?, ?>> nodeClass) {
        super(nodeClass);
        this.schema = schema;
    }

    protected T getSchema() {
        return schema;
    }

    @Override
    public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
        try {
            return Optional.<ModificationApplyOperation> fromNullable(childCache.get(identifier));
        } catch (ExecutionException e) {
            return Optional.absent();
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected abstract DataContainerNodeBuilder createBuilder(NormalizedNode<?, ?> original);

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + schema + "]";
    }

    public static class AugmentationModificationStrategy extends DataNodeContainerModificationStrategy<AugmentationSchema> {

        protected AugmentationModificationStrategy(final AugmentationSchema schema, final DataNodeContainer resolved) {
            super(createAugmentProxy(schema,resolved), AugmentationNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof AugmentationNode);
            return ImmutableAugmentationNodeBuilder.create((AugmentationNode) original);
        }


        private static AugmentationSchema createAugmentProxy(final AugmentationSchema schema, final DataNodeContainer resolved) {
            Set<DataSchemaNode> realChildSchemas = new HashSet<>();
            for(DataSchemaNode augChild : schema.getChildNodes()) {
                realChildSchemas.add(resolved.getDataChildByName(augChild.getQName()));
            }
            return new AugmentationSchemaProxy(schema, realChildSchemas);
        }
    }

    public static class ContainerModificationStrategy extends DataNodeContainerModificationStrategy<ContainerSchemaNode> {

        public ContainerModificationStrategy(final ContainerSchemaNode schemaNode) {
            super(schemaNode, ContainerNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof ContainerNode);
            return ImmutableContainerNodeBuilder.create((ContainerNode) original);
        }
    }

    public static class ListEntryModificationStrategy extends DataNodeContainerModificationStrategy<ListSchemaNode> {

        protected ListEntryModificationStrategy(final ListSchemaNode schema) {
            super(schema, MapEntryNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected final DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof MapEntryNode);
            return ImmutableMapEntryNodeBuilder.create((MapEntryNode) original);
        }
    }

    public static class UnkeyedListItemModificationStrategy extends DataNodeContainerModificationStrategy<ListSchemaNode> {

        public UnkeyedListItemModificationStrategy(final ListSchemaNode schemaNode) {
            super(schemaNode, UnkeyedListEntryNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof UnkeyedListEntryNode);
            return ImmutableUnkeyedListEntryNodeBuilder.create((UnkeyedListEntryNode) original);
        }
    }
}