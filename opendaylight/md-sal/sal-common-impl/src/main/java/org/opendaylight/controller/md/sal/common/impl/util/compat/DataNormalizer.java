/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * @deprecated This class provides compatibility between {@link CompositeNode} and {@link NormalizedNode}.
 *             Users of this class should use {@link NormalizedNode}s directly.
 */
@Deprecated
public class DataNormalizer {

    private final DataNormalizationOperation<?> operation;

    public DataNormalizer(final SchemaContext ctx) {
        operation = DataNormalizationOperation.from(ctx);
    }

    public YangInstanceIdentifier toNormalized(final YangInstanceIdentifier legacy) {
        ImmutableList.Builder<PathArgument> normalizedArgs = ImmutableList.builder();

        DataNormalizationOperation<?> currentOp = operation;
        Iterator<PathArgument> arguments = legacy.getPathArguments().iterator();

        try {
            while (arguments.hasNext()) {
                PathArgument legacyArg = arguments.next();
                currentOp = currentOp.getChild(legacyArg);
                checkArgument(currentOp != null,
                        "Legacy Instance Identifier %s is not correct. Normalized Instance Identifier so far %s",
                        legacy, normalizedArgs.build());
                while (currentOp.isMixin()) {
                    normalizedArgs.add(currentOp.getIdentifier());
                    currentOp = currentOp.getChild(legacyArg.getNodeType());
                }
                normalizedArgs.add(legacyArg);
            }
        } catch (DataNormalizationException e) {
            throw new IllegalArgumentException(String.format("Failed to normalize path %s", legacy), e);
        }

        return YangInstanceIdentifier.create(normalizedArgs.build());
    }

    public DataNormalizationOperation<?> getOperation(final YangInstanceIdentifier legacy) throws DataNormalizationException {
        DataNormalizationOperation<?> currentOp = operation;
        Iterator<PathArgument> arguments = legacy.getPathArguments().iterator();

        while (arguments.hasNext()) {
            currentOp = currentOp.getChild(arguments.next());
        }
        return currentOp;
    }

    public Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalized(
            final Map.Entry<YangInstanceIdentifier, CompositeNode> legacy) {
        return toNormalized(legacy.getKey(), legacy.getValue());
    }

    public Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalized(final YangInstanceIdentifier legacyPath,
            final CompositeNode legacyData) {

        YangInstanceIdentifier normalizedPath = toNormalized(legacyPath);

        DataNormalizationOperation<?> currentOp = operation;
        for (PathArgument arg : normalizedPath.getPathArguments()) {
            try {
                currentOp = currentOp.getChild(arg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Failed to validate normalized path %s",
                        normalizedPath), e);
            }
        }

        Preconditions.checkArgument(currentOp != null,
                "Instance Identifier %s does not reference correct schema Node.", normalizedPath);
        return new AbstractMap.SimpleEntry<YangInstanceIdentifier, NormalizedNode<?, ?>>(normalizedPath,
                currentOp.normalize(legacyData));
    }

    public YangInstanceIdentifier toLegacy(final YangInstanceIdentifier normalized) throws DataNormalizationException {
        ImmutableList.Builder<PathArgument> legacyArgs = ImmutableList.builder();
        DataNormalizationOperation<?> currentOp = operation;
        for (PathArgument normalizedArg : normalized.getPathArguments()) {
            currentOp = currentOp.getChild(normalizedArg);
            if (!currentOp.isMixin()) {
                legacyArgs.add(normalizedArg);
            }
        }
        return YangInstanceIdentifier.create(legacyArgs.build());
    }

    public CompositeNode toLegacy(final YangInstanceIdentifier normalizedPath, final NormalizedNode<?, ?> normalizedData) {
        // Preconditions.checkArgument(normalizedData instanceof
        // DataContainerNode<?>,"Node object %s, %s should be of type DataContainerNode",normalizedPath,normalizedData);
        if (normalizedData instanceof DataContainerNode<?>) {
            return toLegacyFromDataContainer((DataContainerNode<?>) normalizedData);
        } else if (normalizedData instanceof AnyXmlNode) {
            Node<?> value = ((AnyXmlNode) normalizedData).getValue();
            return value instanceof CompositeNode ? (CompositeNode) value : null;
        }
        return null;
    }

    public static Node<?> toLegacy(final NormalizedNode<?, ?> node) {
        if (node instanceof MixinNode) {
            /**
             * Direct reading of MixinNodes is not supported, since it is not
             * possible in legacy APIs create pointer to Mixin Nodes.
             *
             */
            return null;
        }

        if (node instanceof DataContainerNode<?>) {
            return toLegacyFromDataContainer((DataContainerNode<?>) node);
        } else if (node instanceof AnyXmlNode) {
            return ((AnyXmlNode) node).getValue();
        }
        return toLegacySimple(node);

    }

    private static SimpleNode<?> toLegacySimple(final NormalizedNode<?, ?> node) {
        return new SimpleNodeTOImpl<Object>(node.getNodeType(), null, node.getValue());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static CompositeNode toLegacyFromDataContainer(final DataContainerNode<?> node) {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(node.getNodeType());
        for (NormalizedNode<?, ?> child : node.getValue()) {
            if (child instanceof MixinNode && child instanceof NormalizedNodeContainer<?, ?, ?>) {
                builder.addAll(toLegacyNodesFromMixin((NormalizedNodeContainer) child));
            } else if (child instanceof UnkeyedListNode) {
                builder.addAll(toLegacyNodesFromUnkeyedList((UnkeyedListNode) child));
            } else {
                addToBuilder(builder, toLegacy(child));
            }
        }
        return builder.toInstance();
    }

    private static Iterable<? extends Node<?>> toLegacyNodesFromUnkeyedList(final UnkeyedListNode mixin) {
        ArrayList<Node<?>> ret = new ArrayList<>();
        for (NormalizedNode<?, ?> child : mixin.getValue()) {
            ret.add(toLegacy(child));
        }
        return FluentIterable.from(ret).filter(Predicates.notNull());
    }

    private static void addToBuilder(final CompositeNodeBuilder<ImmutableCompositeNode> builder, final Node<?> legacy) {
        if (legacy != null) {
            builder.add(legacy);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Iterable<Node<?>> toLegacyNodesFromMixin(
            final NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> mixin) {
        ArrayList<Node<?>> ret = new ArrayList<>();
        for (NormalizedNode<?, ?> child : mixin.getValue()) {
            if (child instanceof MixinNode && child instanceof NormalizedNodeContainer<?, ?, ?>) {
                Iterables.addAll(ret, toLegacyNodesFromMixin((NormalizedNodeContainer) child));
            } else {
                ret.add(toLegacy(child));
            }
        }
        return FluentIterable.from(ret).filter(Predicates.notNull());
    }

    public DataNormalizationOperation<?> getRootOperation() {
        return operation;
    }

}
