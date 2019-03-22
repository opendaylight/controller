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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeBuilder;
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

    @Override
    public void startLeafNode(final NodeIdentifier name) {
        addBuilder(Builders.leafBuilder().withNodeIdentifier(name), name);
    }

    @Override
    public void startLeafSet(final NodeIdentifier nodeIdentifier, final int count) {
        addBuilder(Builders.leafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier nodeIdentifier, final int str) {
        addBuilder(Builders.orderedLeafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        addBuilder(Builders.leafSetEntryBuilder().withNodeIdentifier(name), name);
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

    @Override
    public void startAnyxmlNode(final NodeIdentifier name) {
        addBuilder(Builders.anyXmlBuilder().withNodeIdentifier(name), name);
    }

    @Override
    public void domSourceValue(final DOMSource value) {
        setValue(value);
    }

    @Override
    public void scalarValue(final Object value) {
        setValue(value);
    }

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

        final NormalizedNode<?, ?> newNode = child.build();
        final NormalizedNodeBuilderWrapper parent = stack.peek();
        if (parent == null) {
            normalizedNode = newNode;
            state = State.CLOSED;
        } else {
            parent.addChild(newNode);
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

    private void setValue(final Object value) {
        checkNotSealed();
        final NormalizedNodeBuilderWrapper current = stack.peek();
        checkState(current != null, "Attempted to set value %s while no node is open", value);
        current.setValue(value);
    }

    private <T extends NormalizedNodeBuilder<?, ?, ?>> NormalizedNodeBuilderWrapper addBuilder(final T builder,
            final PathArgument identifier) {
        checkNotSealed();

        final DataSchemaContextNode<?> schemaNode;
        final NormalizedNodeBuilderWrapper parent = stack.peek();
        if (parent != null) {
            schemaNode = parent.childSchema(identifier);
        } else {
            schemaNode = nodePathSchemaNode;
        }

        NormalizedNodeBuilderWrapper wrapper = new NormalizedNodeBuilderWrapper(builder, identifier, schemaNode);
        stack.push(wrapper);
        return wrapper;
    }
}
