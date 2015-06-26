/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util;

import static org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter.UNKNOWN_SIZE;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamAttributeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

//TODO this does not extend NormalizedNodeWriter from yangtools due to api freeze, make this inherit common methods to avoid code duplication
//TODO move this to yangtools, since this is in netconf-util due to api freeze in lithium
public class OrderedNormalizedNodeWriter implements Closeable, Flushable{

    private final SchemaContext schemaContext;
    private final SchemaNode root;
    private final NormalizedNodeStreamWriter writer;

    public OrderedNormalizedNodeWriter(NormalizedNodeStreamWriter writer, SchemaContext schemaContext, SchemaPath path) {
        this.writer = writer;
        this.schemaContext = schemaContext;
        this.root = findParentSchemaOnPath(schemaContext, path);
    }

    public OrderedNormalizedNodeWriter write(final NormalizedNode<?, ?> node) throws IOException {
        if (root == schemaContext) {
            return write(node, schemaContext.getDataChildByName(node.getNodeType()));
        }

        return write(node, root);
    }

    public OrderedNormalizedNodeWriter write(final Collection<DataContainerChild<?,?>> nodes) throws IOException {
        if (writeChildren(nodes, root, false)) {
            return this;
        }

        throw new IllegalStateException("It wasn't possible to serialize nodes " + nodes);

    }

    private OrderedNormalizedNodeWriter write(NormalizedNode<?, ?> node, SchemaNode dataSchemaNode) throws IOException {
        if (node == null) {
            return this;
        }

        if (wasProcessedAsCompositeNode(node, dataSchemaNode)) {
            return this;
        }

        if (wasProcessAsSimpleNode(node)) {
            return this;
        }

        throw new IllegalStateException("It wasn't possible to serialize node " + node);
    }

    private void write(List<NormalizedNode<?, ?>> nodes, SchemaNode dataSchemaNode) throws IOException {
        for (NormalizedNode<?, ?> node : nodes) {
            write(node, dataSchemaNode);
        }
    }

    private OrderedNormalizedNodeWriter writeLeaf(final NormalizedNode<?, ?> node) throws IOException {
        if (wasProcessAsSimpleNode(node)) {
            return this;
        }

        throw new IllegalStateException("It wasn't possible to serialize node " + node);
    }

    private boolean writeChildren(final Iterable<? extends NormalizedNode<?, ?>> children, SchemaNode parentSchemaNode, boolean endParent) throws IOException {
        //Augmentations cannot be gotten with node.getChild so create our own structure with augmentations resolved
        ArrayListMultimap<QName, NormalizedNode<?, ?>> qNameToNodes = ArrayListMultimap.create();
        for (NormalizedNode<?, ?> child : children) {
            if (child instanceof AugmentationNode) {
                qNameToNodes.putAll(resolveAugmentations(child));
            } else {
                qNameToNodes.put(child.getNodeType(), child);
            }
        }

        if (parentSchemaNode instanceof DataNodeContainer) {
            if (parentSchemaNode instanceof ListSchemaNode && qNameToNodes.containsKey(parentSchemaNode.getQName())) {
                write(qNameToNodes.get(parentSchemaNode.getQName()), parentSchemaNode);
            } else {
                for (DataSchemaNode schemaNode : ((DataNodeContainer) parentSchemaNode).getChildNodes()) {
                    write(qNameToNodes.get(schemaNode.getQName()), schemaNode);
                }
            }
        } else if(parentSchemaNode instanceof ChoiceSchemaNode) {
            for (ChoiceCaseNode ccNode : ((ChoiceSchemaNode) parentSchemaNode).getCases()) {
                for (DataSchemaNode dsn : ccNode.getChildNodes()) {
                    if (qNameToNodes.containsKey(dsn.getQName())) {
                        write(qNameToNodes.get(dsn.getQName()), dsn);
                    }
                }
            }
        } else {
            for (NormalizedNode<?, ?> child : children) {
                writeLeaf(child);
            }
        }
        if (endParent) {
            writer.endNode();
        }
        return true;
    }

    private ArrayListMultimap<QName, NormalizedNode<?, ?>> resolveAugmentations(NormalizedNode<?, ?> child) {
        final ArrayListMultimap<QName, NormalizedNode<?, ?>> resolvedAugs = ArrayListMultimap.create();
        for (NormalizedNode<?, ?> node : ((AugmentationNode) child).getValue()) {
            if (node instanceof AugmentationNode) {
                resolvedAugs.putAll(resolveAugmentations(node));
            } else {
                resolvedAugs.put(node.getNodeType(), node);
            }
        }
        return resolvedAugs;
    }

    private boolean writeMapEntryNode(final MapEntryNode node, final SchemaNode dataSchemaNode) throws IOException {
        if(writer instanceof NormalizedNodeStreamAttributeWriter) {
            ((NormalizedNodeStreamAttributeWriter) writer)
                    .startMapEntryNode(node.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(node.getValue()), node.getAttributes());
        } else {
            writer.startMapEntryNode(node.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(node.getValue()));
        }
        return writeChildren(node.getValue(), dataSchemaNode, true);
    }

    private boolean wasProcessAsSimpleNode(final NormalizedNode<?, ?> node) throws IOException {
        if (node instanceof LeafSetEntryNode) {
            final LeafSetEntryNode<?> nodeAsLeafList = (LeafSetEntryNode<?>)node;
            if(writer instanceof NormalizedNodeStreamAttributeWriter) {
                ((NormalizedNodeStreamAttributeWriter) writer).leafSetEntryNode(nodeAsLeafList.getValue(), nodeAsLeafList.getAttributes());
            } else {
                writer.leafSetEntryNode(nodeAsLeafList.getValue());
            }
            return true;
        } else if (node instanceof LeafNode) {
            final LeafNode<?> nodeAsLeaf = (LeafNode<?>)node;
            if(writer instanceof NormalizedNodeStreamAttributeWriter) {
                ((NormalizedNodeStreamAttributeWriter) writer).leafNode(nodeAsLeaf.getIdentifier(), nodeAsLeaf.getValue(), nodeAsLeaf.getAttributes());
            } else {
                writer.leafNode(nodeAsLeaf.getIdentifier(), nodeAsLeaf.getValue());
            }
            return true;
        } else if (node instanceof AnyXmlNode) {
            final AnyXmlNode anyXmlNode = (AnyXmlNode)node;
            writer.anyxmlNode(anyXmlNode.getIdentifier(), anyXmlNode.getValue());
            return true;
        }

        return false;
    }

    private boolean wasProcessedAsCompositeNode(final NormalizedNode<?, ?> node, SchemaNode dataSchemaNode) throws IOException {
        if (node instanceof ContainerNode) {
            final ContainerNode n = (ContainerNode) node;
            if(writer instanceof NormalizedNodeStreamAttributeWriter) {
                ((NormalizedNodeStreamAttributeWriter) writer).startContainerNode(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()), n.getAttributes());
            } else {
                writer.startContainerNode(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            }
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof MapEntryNode) {
            return writeMapEntryNode((MapEntryNode) node, dataSchemaNode);
        }
        if (node instanceof UnkeyedListEntryNode) {
            final UnkeyedListEntryNode n = (UnkeyedListEntryNode) node;
            writer.startUnkeyedListItem(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof ChoiceNode) {
            final ChoiceNode n = (ChoiceNode) node;
            writer.startChoiceNode(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof AugmentationNode) {
            final AugmentationNode n = (AugmentationNode) node;
            writer.startAugmentationNode(n.getIdentifier());
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof UnkeyedListNode) {
            final UnkeyedListNode n = (UnkeyedListNode) node;
            writer.startUnkeyedList(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof OrderedMapNode) {
            final OrderedMapNode n = (OrderedMapNode) node;
            writer.startOrderedMapNode(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof MapNode) {
            final MapNode n = (MapNode) node;
            writer.startMapNode(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }
        if (node instanceof LeafSetNode) {
            //covers also OrderedLeafSetNode for which doesn't exist start* method
            final LeafSetNode<?> n = (LeafSetNode<?>) node;
            writer.startLeafSet(n.getIdentifier(), OrderedNormalizedNodeWriter.childSizeHint(n.getValue()));
            return writeChildren(n.getValue(), dataSchemaNode, true);
        }

        return false;
    }

    private static final int childSizeHint(final Iterable<?> children) {
        return (children instanceof Collection) ? ((Collection<?>) children).size() : UNKNOWN_SIZE;
    }

    //TODO similar code is already present in schemaTracker, unify this when this writer is moved back to yangtools
    private SchemaNode findParentSchemaOnPath(SchemaContext schemaContext, SchemaPath path) {
        SchemaNode current = Preconditions.checkNotNull(schemaContext);
        for (final QName qname : path.getPathFromRoot()) {
            SchemaNode child;
            if(current instanceof DataNodeContainer) {
                child = ((DataNodeContainer) current).getDataChildByName(qname);

                if (child == null && current instanceof SchemaContext) {
                    child = tryFindGroupings((SchemaContext) current, qname).orNull();
                }

                if(child == null && current instanceof SchemaContext) {
                    child = tryFindNotification((SchemaContext) current, qname)
                            .or(tryFindRpc(((SchemaContext) current), qname)).orNull();
                }
            } else if (current instanceof ChoiceSchemaNode) {
                child = ((ChoiceSchemaNode) current).getCaseNodeByName(qname);
            } else if (current instanceof RpcDefinition) {
                switch (qname.getLocalName()) {
                case "input":
                    child = ((RpcDefinition) current).getInput();
                    break;
                case "output":
                    child = ((RpcDefinition) current).getOutput();
                    break;
                default:
                    child = null;
                    break;
                }
            } else {
                throw new IllegalArgumentException(String.format("Schema node %s does not allow children.", current));
            }
            current = child;
        }
        return current;
    }

    //TODO this method is already present in schemaTracker, unify this when this writer is moved back to yangtools
    private Optional<SchemaNode> tryFindGroupings(final SchemaContext ctx, final QName qname) {
        return Optional.<SchemaNode> fromNullable(Iterables.find(ctx.getGroupings(), new SchemaNodePredicate(qname), null));
    }

    //TODO this method is already present in schemaTracker, unify this when this writer is moved back to yangtools
    private Optional<SchemaNode> tryFindRpc(final SchemaContext ctx, final QName qname) {
        return Optional.<SchemaNode>fromNullable(Iterables.find(ctx.getOperations(), new SchemaNodePredicate(qname), null));
    }

    //TODO this method is already present in schemaTracker, unify this when this writer is moved back to yangtools
    private Optional<SchemaNode> tryFindNotification(final SchemaContext ctx, final QName qname) {
        return Optional.<SchemaNode>fromNullable(Iterables.find(ctx.getNotifications(), new SchemaNodePredicate(qname), null));
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

    //TODO this class is already present in schemaTracker, unify this when this writer is moved back to yangtools
    private static final class SchemaNodePredicate implements Predicate<SchemaNode> {
        private final QName qname;

        public SchemaNodePredicate(final QName qname) {
            this.qname = qname;
        }

        @Override
        public boolean apply(final SchemaNode input) {
            return input.getQName().equals(qname);
        }
    }
}