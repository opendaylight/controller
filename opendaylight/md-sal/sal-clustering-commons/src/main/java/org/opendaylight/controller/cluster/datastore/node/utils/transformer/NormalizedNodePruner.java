/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
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
    private final SimpleStack<NormalizedNodeBuilderWrapper> stack = new SimpleStack<>();
    private NormalizedNode<?,?> normalizedNode;
    private final DataSchemaContextNode<?> nodePathSchemaNode;
    private boolean sealed = false;

    public NormalizedNodePruner(YangInstanceIdentifier nodePath, SchemaContext schemaContext) {
        nodePathSchemaNode = findSchemaNodeForNodePath(nodePath, schemaContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leafNode(NodeIdentifier nodeIdentifier, Object value) {

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
    public void startLeafSet(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.leafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startOrderedLeafSet(NodeIdentifier nodeIdentifier, int str) {
        checkNotSealed();

        addBuilder(Builders.orderedLeafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startLeafSetEntryNode(NodeWithValue<?> name) {
        checkNotSealed();

//        addBuilder(Builders.leafSetEntryBuilder().withNodeIdentifier(name), name);

//      NormalizedNodeBuilderWrapper parent = stack.peek();
//      if (parent != null) {
//          if (hasValidSchema(name, parent)) {
//              parent.builder().addChild(Builders.leafSetEntryBuilder().withValue(value)
//                      .withNodeIdentifier(new NodeWithValue<>(parent.nodeType(), value))
//                      .build());
//          }
//      } else {
//          // If there's no parent LeafSetNode then this is a stand alone
//          // LeafSetEntryNode.
//          if (nodePathSchemaNode != null) {
//              this.normalizedNode = Builders.leafSetEntryBuilder().withValue(value).withNodeIdentifier(
//                      new NodeWithValue<>(name, value)).build();
//          }
//
//          sealed = true;
//      }

    }

    @Override
    public void startContainerNode(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.containerBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startYangModeledAnyXmlNode(NodeIdentifier nodeIdentifier, int count) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void startUnkeyedList(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.unkeyedListBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startUnkeyedListItem(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.unkeyedListEntryBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapNode(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.mapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapEntryNode(NodeIdentifierWithPredicates nodeIdentifierWithPredicates, int count) {
        checkNotSealed();

        addBuilder(Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifierWithPredicates),
                nodeIdentifierWithPredicates);
    }

    @Override
    public void startOrderedMapNode(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.orderedMapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startChoiceNode(NodeIdentifier nodeIdentifier, int count) {
        checkNotSealed();

        addBuilder(Builders.choiceBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startAugmentationNode(AugmentationIdentifier augmentationIdentifier) {

        checkNotSealed();

        addBuilder(Builders.augmentationBuilder().withNodeIdentifier(augmentationIdentifier), augmentationIdentifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void anyxmlNode(NodeIdentifier nodeIdentifier, Object value) {
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

        NormalizedNodeBuilderWrapper child = stack.pop();

        Preconditions.checkState(child != null, "endNode called on an empty stack");

        if (!child.getSchema().isPresent()) {
            LOG.debug("Schema not found for {}", child.identifier());
            return;
        }

        NormalizedNode<?,?> newNode = child.builder().build();

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

    }

    public NormalizedNode<?,?> normalizedNode() {
        return normalizedNode;
    }

    private void checkNotSealed() {
        Preconditions.checkState(!sealed, "Pruner can be used only once");
    }

    private static boolean hasValidSchema(QName name, NormalizedNodeBuilderWrapper parent) {
        boolean valid = parent.getSchema().isPresent() && parent.getSchema().get().getChild(name) != null;
        if (!valid) {
            LOG.debug("Schema not found for {}", name);
        }

        return valid;
    }

    private NormalizedNodeBuilderWrapper addBuilder(NormalizedNodeContainerBuilder<?,?,?,?> builder,
            PathArgument identifier) {
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

    private static DataSchemaContextNode<?> findSchemaNodeForNodePath(YangInstanceIdentifier nodePath,
            SchemaContext schemaContext) {
        DataSchemaContextNode<?> schemaNode = DataSchemaContextTree.from(schemaContext).getRoot();
        for (PathArgument arg : nodePath.getPathArguments()) {
            schemaNode = schemaNode.getChild(arg);
            if (schemaNode == null) {
                break;
            }
        }

        return schemaNode;
    }

    @VisibleForTesting
    static class SimpleStack<E> {
        List<E> stack = new LinkedList<>();

        void push(E element) {
            stack.add(element);
        }

        E pop() {
            if (size() == 0) {
                return null;
            }
            return stack.remove(stack.size() - 1);
        }

        E peek() {
            if (size() == 0) {
                return null;
            }

            return stack.get(stack.size() - 1);
        }

        int size() {
            return stack.size();
        }
    }
}
