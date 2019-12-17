/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
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
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
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

    @FunctionalInterface
    interface WriterMethod<T extends PathArgument> {

        void apply(ReusableImmutableNormalizedNodeStreamWriter writer, T name) throws IOException;
    }

    @FunctionalInterface
    interface SizedWriterMethod<T extends PathArgument> {

        void apply(ReusableImmutableNormalizedNodeStreamWriter writer, T name, int childSizeHint) throws IOException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNormalizedNodePruner.class);

    private final Deque<DataSchemaContextNode<?>> stack = new ArrayDeque<>();
    private final ReusableImmutableNormalizedNodeStreamWriter delegate =
            ReusableImmutableNormalizedNodeStreamWriter.create();
    private final DataSchemaContextTree tree;

    private DataSchemaContextNode<?> nodePathSchemaNode;
    private State state = State.UNITIALIZED;
    private int unknown;

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
        unknown = 0;
        normalizedNode = null;
        stack.clear();
        delegate.reset();
        state = State.OPEN;
    }

    @Override
    public void startLeafNode(final NodeIdentifier name) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startLeafNode, name);
    }

    @Override
    public void startLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startLeafSet, name, childSizeHint);
    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startOrderedLeafSet, name, childSizeHint);
    }

    @Override
    public void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startLeafSetEntryNode, name);
    }

    @Override
    public void startContainerNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startContainerNode, name, childSizeHint);
    }

    @Override
    public void startYangModeledAnyXmlNode(final NodeIdentifier nodeIdentifier, final int count) {
        // FIXME: implement this
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startUnkeyedList, name, childSizeHint);
    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startUnkeyedListItem, name, childSizeHint);
    }

    @Override
    public void startMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startMapNode, name, childSizeHint);
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startMapEntryNode, identifier, childSizeHint);
    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startOrderedMapNode, name, childSizeHint);
    }

    @Override
    public void startChoiceNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startChoiceNode, name, childSizeHint);
    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier identifier) throws IOException {
        enter(ReusableImmutableNormalizedNodeStreamWriter::startAugmentationNode, identifier);
    }

    @Override
    public boolean startAnyxmlNode(final NodeIdentifier name, final Class<?> objectModel) throws IOException {
        if (enter(name)) {
            verify(delegate.startAnyxmlNode(name, objectModel),
                "Unexpected failure to stream DOMSource node %s model %s", name, objectModel);
        }
        return true;
    }

    @Override
    public boolean startAnydataNode(final NodeIdentifier name, final Class<?> objectModel) throws IOException {
        // FIXME: we do not support anydata nodes yet
        return false;
    }

    @Override
    public void domSourceValue(final DOMSource value) throws IOException {
        checkNotSealed();
        if (unknown == 0) {
            delegate.domSourceValue(value);
        }
    }

    @Override
    public void scalarValue(final Object value) throws IOException {
        checkNotSealed();
        if (unknown == 0) {
            delegate.scalarValue(translateScalar(currentSchema(), value));
        }
    }

    Object translateScalar(final DataSchemaContextNode<?> context, final Object value) throws IOException {
        // Default is pass-through
        return value;
    }

    @Override
    public void endNode() throws IOException {
        checkNotSealed();

        if (unknown == 0) {
            try {
                stack.pop();
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("endNode called on an empty stack", e);
            }
            delegate.endNode();
        } else {
            unknown--;
            if (unknown != 0) {
                // Still at unknown, do not attempt to create result
                return;
            }
        }

        if (stack.isEmpty()) {
            normalizedNode = delegate.getResult();
            state = State.CLOSED;
        }
    }

    @Override
    public void close() throws IOException {
        state = State.CLOSED;
        stack.clear();
        delegate.close();
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
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

    private boolean enter(final PathArgument name) {
        checkNotSealed();

        if (unknown != 0) {
            LOG.debug("Skipping child {} in unknown subtree", name);
            unknown++;
            return false;
        }

        final DataSchemaContextNode<?> schema;
        final DataSchemaContextNode<?> parent = currentSchema();
        if (parent != null) {
            schema = parent.getChild(name);
        } else {
            schema = nodePathSchemaNode;
        }

        if (schema == null) {
            LOG.debug("Schema not found for {}", name);
            unknown = 1;
            return false;
        }

        stack.push(schema);
        final DataSchemaNode dataSchema = schema.getDataSchemaNode();
        if (dataSchema != null) {
            delegate.nextDataSchemaNode(dataSchema);
        }
        return true;
    }

    final <A extends PathArgument> void enter(final WriterMethod<A> method, final A name) throws IOException {
        if (enter(name)) {
            method.apply(delegate, name);
        }
    }

    final <A extends PathArgument> void enter(final SizedWriterMethod<A> method, final A name, final int size)
            throws IOException {
        if (enter(name)) {
            method.apply(delegate, name, size);
        }
    }

    final DataSchemaContextNode<?> currentSchema() {
        return stack.peek();
    }
}
