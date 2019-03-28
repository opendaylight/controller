/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The NormalizedNodePruner removes all nodes from the input NormalizedNode that do not have a corresponding
 * schema element in the passed in SchemaContext.
 */
abstract class AbstractNormalizedNodePruner implements NormalizedNodeStreamWriter {
    enum State {
        UNITIALIZED,
        OPEN,
        CLOSED;
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNormalizedNodePruner.class);

    private final Deque<NormalizedNodeBuilderWrapper> stack = new ArrayDeque<>();
    private final DataSchemaContextTree tree;

    private DataSchemaContextNode<?> nodePathSchemaNode;
    private State state = State.UNITIALIZED;

    // FIXME: package-private to support unguarded NormalizedNodePruner access
    NormalizedNode<?, ?> normalizedNode;

    AbstractNormalizedNodePruner(final DataSchemaContextTree tree) {
        this.tree = requireNonNull(tree);
    }

    AbstractNormalizedNodePruner(final SchemaContext schemaContext) {
        this(DataSchemaContextTree.from(schemaContext));
    }

    final DataSchemaContextTree getTree() {
        return tree;
    }

    final void initialize(final YangInstanceIdentifier nodePath) {
        nodePathSchemaNode = tree.findChild(nodePath).orElse(null);
        normalizedNode = null;
        stack.clear();
        state = State.OPEN;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leafNode(final NodeIdentifier nodeIdentifier, final Object value) {
        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        LeafNode<Object> leafNode = Builders.leafBuilder().withNodeIdentifier(nodeIdentifier).withValue(value).build();
        if (parent != null) {
            if (hasValidSchema(nodeIdentifier.getNodeType(), parent)) {
                parent.builder().addChild(leafNode);
            }
        } else {
            // If there's no parent node then this is a stand alone LeafNode.
            if (nodePathSchemaNode != null) {
                this.normalizedNode = leafNode;
            }

            state = State.CLOSED;
        }
    }

    @Override
    public void startLeafSet(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.leafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier nodeIdentifier, final int str) {
        addBuilder(Builders.orderedLeafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leafSetEntryNode(final QName name, final Object value) {
        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        if (parent != null) {
            if (hasValidSchema(name, parent)) {
                parent.builder().addChild(Builders.leafSetEntryBuilder().withValue(value)
                        .withNodeIdentifier(new NodeWithValue<>(parent.nodeType(), value))
                        .build());
            }
        } else {
            // If there's no parent LeafSetNode then this is a stand alone
            // LeafSetEntryNode.
            if (nodePathSchemaNode != null) {
                this.normalizedNode = Builders.leafSetEntryBuilder().withValue(value).withNodeIdentifier(
                        new NodeWithValue<>(name, value)).build();
            }

            state = State.CLOSED;
        }
    }

    @Override
    public void startContainerNode(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.containerBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startYangModeledAnyXmlNode(final NodeIdentifier nodeIdentifier, final int count) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void startUnkeyedList(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.unkeyedListBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.unkeyedListEntryBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapNode(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.mapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates nodeIdentifierWithPredicates, final int count) {
        addBuilder(Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifierWithPredicates),
                nodeIdentifierWithPredicates);
    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.orderedMapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startChoiceNode(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.choiceBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier augmentationIdentifier) {
        addBuilder(Builders.augmentationBuilder().withNodeIdentifier(augmentationIdentifier), augmentationIdentifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void anyxmlNode(final NodeIdentifier nodeIdentifier, final Object value) {
        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        AnyXmlNode anyXmlNode = Builders.anyXmlBuilder().withNodeIdentifier(nodeIdentifier).withValue((DOMSource) value)
                .build();
        if (parent != null) {
            if (hasValidSchema(nodeIdentifier.getNodeType(), parent)) {
                parent.builder().addChild(anyXmlNode);
            }
        } else {
            // If there's no parent node then this is a stand alone AnyXmlNode.
            if (nodePathSchemaNode != null) {
                this.normalizedNode = anyXmlNode;
            }

            state = State.CLOSED;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void endNode() {
        checkNotSealed();

        final NormalizedNodeBuilderWrapper child;
        try {
            child = stack.pop();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("endNode called on an empty stack", e);
        }

        if (child.getSchema() == null) {
            LOG.debug("Schema not found for {}", child.identifier());
            if (stack.isEmpty()) {
                normalizedNode = null;
                state = State.CLOSED;
            }
            return;
        }

        final NormalizedNode<?, ?> newNode = child.builder().build();
        final NormalizedNodeBuilderWrapper parent = stack.peek();
        if (parent == null) {
            normalizedNode = newNode;
            state = State.CLOSED;
        } else {
            parent.builder().addChild(newNode);
        }
    }

    @Override
    public void close() {
        state = State.CLOSED;
        stack.clear();
    }

    @Override
    public void flush() {
        // No-op
    }

    /**
     * Return the resulting normalized node.
     *
     * @return Resulting node for the path, if it was not pruned
     * @throws IllegalStateException if this pruner has not been closed
     */
    public final Optional<NormalizedNode<?, ?>> getResult() {
        checkState(state == State.CLOSED, "Cannot get result in state %s", state);
        return Optional.ofNullable(normalizedNode);
    }

    private void checkNotSealed() {
        checkState(state == State.OPEN, "Illegal operation in state %s", state);
    }

    private static boolean hasValidSchema(final QName name, final NormalizedNodeBuilderWrapper parent) {
        final DataSchemaContextNode<?> parentSchema = parent.getSchema();
        final boolean valid = parentSchema != null && parentSchema.getChild(name) != null;
        if (!valid) {
            LOG.debug("Schema not found for {}", name);
        }

        return valid;
    }

    private NormalizedNodeBuilderWrapper addBuilder(final NormalizedNodeContainerBuilder<?, ?, ?, ?> builder,
            final PathArgument identifier) {
        checkNotSealed();

        final DataSchemaContextNode<?> schemaNode;
        final NormalizedNodeBuilderWrapper parent = stack.peek();
        if (parent != null) {
            final DataSchemaContextNode<?> parentSchema = parent.getSchema();
            schemaNode = parentSchema == null ? null : parentSchema.getChild(identifier);
        } else {
            schemaNode = nodePathSchemaNode;
        }

        NormalizedNodeBuilderWrapper wrapper = new NormalizedNodeBuilderWrapper(builder, identifier, schemaNode);
        stack.push(wrapper);
        return wrapper;
    }
}
