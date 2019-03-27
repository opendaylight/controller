/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
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
public class NormalizedNodePruner implements NormalizedNodeStreamWriter {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodePruner.class);

    public static final URI BASE_NAMESPACE = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");

    private final Deque<NormalizedNodeBuilderWrapper> stack = new ArrayDeque<>();
    private final DataSchemaContextNode<?> nodePathSchemaNode;

    private NormalizedNode<?, ?> normalizedNode;
    private boolean sealed = false;

    public NormalizedNodePruner(final YangInstanceIdentifier nodePath, final SchemaContext schemaContext) {
        nodePathSchemaNode = findSchemaNodeForNodePath(nodePath, schemaContext);
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

            sealed = true;
        }
    }

    @Override
    public void startLeafSet(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.leafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier nodeIdentifier, final int str) {
        checkNotSealed();
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

            sealed = true;
        }
    }

    @Override
    public void startContainerNode(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.containerBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startYangModeledAnyXmlNode(final NodeIdentifier nodeIdentifier, final int count) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void startUnkeyedList(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.unkeyedListBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.unkeyedListEntryBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapNode(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.mapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates nodeIdentifierWithPredicates, final int count) {
        checkNotSealed();
        addBuilder(Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifierWithPredicates),
                nodeIdentifierWithPredicates);
    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.orderedMapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startChoiceNode(final NodeIdentifier nodeIdentifier, final int count) {
        checkNotSealed();
        addBuilder(Builders.choiceBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier augmentationIdentifier) {
        checkNotSealed();
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

            sealed = true;
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

        if (!child.getSchema().isPresent()) {
            LOG.debug("Schema not found for {}", child.identifier());
            return;
        }

        NormalizedNode<?, ?> newNode = child.builder().build();
        if (stack.size() > 0) {
            NormalizedNodeBuilderWrapper parent = stack.peek();
            parent.builder().addChild(newNode);
        } else {
            this.normalizedNode = newNode;
            sealed = true;
        }
    }

    @Override
    public void close() {
        sealed = true;
    }

    @Override
    public void flush() {
        // No-op
    }

    public NormalizedNode<?, ?> normalizedNode() {
        return normalizedNode;
    }

    private void checkNotSealed() {
        checkState(!sealed, "Pruner can be used only once");
    }

    private static boolean hasValidSchema(final QName name, final NormalizedNodeBuilderWrapper parent) {
        boolean valid = parent.getSchema().isPresent() && parent.getSchema().get().getChild(name) != null;
        if (!valid) {
            LOG.debug("Schema not found for {}", name);
        }

        return valid;
    }

    private NormalizedNodeBuilderWrapper addBuilder(final NormalizedNodeContainerBuilder<?, ?, ?, ?> builder,
            final PathArgument identifier) {
        final Optional<DataSchemaContextNode<?>> schemaNode;
        NormalizedNodeBuilderWrapper parent = stack.peek();
        if (parent == null) {
            schemaNode = Optional.fromNullable(nodePathSchemaNode);
        } else if (parent.getSchema().isPresent()) {
            schemaNode = Optional.fromNullable(parent.getSchema().get().getChild(identifier));
        } else {
            schemaNode = Optional.absent();
        }

        NormalizedNodeBuilderWrapper wrapper = new NormalizedNodeBuilderWrapper(builder, identifier, schemaNode);
        stack.push(wrapper);
        return wrapper;
    }

    private static DataSchemaContextNode<?> findSchemaNodeForNodePath(final YangInstanceIdentifier nodePath,
            final SchemaContext schemaContext) {
        DataSchemaContextNode<?> schemaNode = DataSchemaContextTree.from(schemaContext).getRoot();
        for (PathArgument arg : nodePath.getPathArguments()) {
            schemaNode = schemaNode.getChild(arg);
            if (schemaNode == null) {
                break;
            }
        }

        return schemaNode;
    }
}
