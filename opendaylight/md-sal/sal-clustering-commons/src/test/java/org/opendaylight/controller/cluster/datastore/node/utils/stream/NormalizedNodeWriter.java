/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;

import static org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeStreamWriter.UNKNOWN_SIZE;


/**
 * This class is used only for testing purpose for now, we may use similar logic while integrating
 * with cluster
 */

public class NormalizedNodeWriter implements Closeable, Flushable {
    private final NormalizedNodeStreamWriter writer;

    private NormalizedNodeWriter(final NormalizedNodeStreamWriter writer) {
        this.writer = Preconditions.checkNotNull(writer);
    }

    protected final NormalizedNodeStreamWriter getWriter() {
        return writer;
    }

    /**
     * Create a new writer backed by a {@link org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter}.
     *
     * @param writer Back-end writer
     * @return A new instance.
     */
    public static NormalizedNodeWriter forStreamWriter(final NormalizedNodeStreamWriter writer) {
        return new NormalizedNodeWriter(writer);
    }


    /**
     * Iterate over the provided {@link org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode} and emit write
     * events to the encapsulated {@link org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter}.
     *
     * @param node Node
     * @return
     * @throws java.io.IOException when thrown from the backing writer.
     */
    public final NormalizedNodeWriter write(final NormalizedNode<?, ?> node) throws IOException {
        if (wasProcessedAsComplexNode(node)) {
            return this;
        }

        if (wasProcessAsSimpleNode(node)) {
            return this;
        }

        throw new IllegalStateException("It wasn't possible to serialize node " + node);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

    /**
     * Emit a best guess of a hint for a particular set of children. It evaluates the
     * iterable to see if the size can be easily gotten to. If it is, we hint at the
     * real number of child nodes. Otherwise we emit UNKNOWN_SIZE.
     *
     * @param children Child nodes
     * @return Best estimate of the collection size required to hold all the children.
     */
    static final int childSizeHint(final Iterable<?> children) {
        return (children instanceof Collection) ? ((Collection<?>) children).size() : UNKNOWN_SIZE;
    }

    private boolean wasProcessAsSimpleNode(final NormalizedNode<?, ?> node) throws IOException {
        if (node instanceof LeafSetEntryNode) {
            final LeafSetEntryNode<?> nodeAsLeafList = (LeafSetEntryNode<?>)node;
            writer.leafSetEntryNode(nodeAsLeafList.getIdentifier(), nodeAsLeafList.getValue());
            return true;
        } else if (node instanceof LeafNode) {
            final LeafNode<?> nodeAsLeaf = (LeafNode<?>)node;
            writer.leafNode(nodeAsLeaf.getIdentifier(), nodeAsLeaf.getValue());
            return true;
        } else if (node instanceof AnyXmlNode) {
            final AnyXmlNode anyXmlNode = (AnyXmlNode)node;
            writer.anyxmlNode(anyXmlNode.getIdentifier(), anyXmlNode.getValue());
            return true;
        }

        return false;
    }

    /**
     * Emit events for all children and then emit an endNode() event.
     *
     * @param children Child iterable
     * @return True
     * @throws java.io.IOException when the writer reports it
     */
    protected final boolean writeChildren(final Iterable<? extends NormalizedNode<?, ?>> children) throws IOException {
        for (NormalizedNode<?, ?> child : children) {
            write(child);
        }

        writer.endNode();
        return true;
    }

    protected boolean writeMapEntryNode(final MapEntryNode node) throws IOException {
        writer.startMapEntryNode(node.getIdentifier(), childSizeHint(node.getValue()));
        return writeChildren(node.getValue());
    }

    private boolean wasProcessedAsComplexNode(final NormalizedNode<?, ?> node) throws IOException {
        if (node instanceof ContainerNode) {
            final ContainerNode n = (ContainerNode) node;
            writer.startContainerNode(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }
        if (node instanceof MapEntryNode) {
            return writeMapEntryNode((MapEntryNode) node);
        }
        if (node instanceof UnkeyedListEntryNode) {
            final UnkeyedListEntryNode n = (UnkeyedListEntryNode) node;
            writer.startUnkeyedListItem(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }
        if (node instanceof ChoiceNode) {
            final ChoiceNode n = (ChoiceNode) node;
            writer.startChoiceNode(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }
        if (node instanceof AugmentationNode) {
            final AugmentationNode n = (AugmentationNode) node;
            writer.startAugmentationNode(n.getIdentifier());
            return writeChildren(n.getValue());
        }
        if (node instanceof UnkeyedListNode) {
            final UnkeyedListNode n = (UnkeyedListNode) node;
            writer.startUnkeyedList(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }
        if (node instanceof OrderedMapNode) {
            final OrderedMapNode n = (OrderedMapNode) node;
            writer.startOrderedMapNode(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }
        if (node instanceof MapNode) {
            final MapNode n = (MapNode) node;
            writer.startMapNode(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }
        if (node instanceof LeafSetNode) {
            //covers also OrderedLeafSetNode for which doesn't exist start* method
            final LeafSetNode<?> n = (LeafSetNode<?>) node;
            writer.startLeafSet(n.getIdentifier(), childSizeHint(n.getValue()));
            return writeChildren(n.getValue());
        }

        return false;
    }
}
