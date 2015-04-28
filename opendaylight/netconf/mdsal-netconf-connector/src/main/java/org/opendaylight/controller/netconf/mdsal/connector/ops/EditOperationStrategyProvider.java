/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditConfigInput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.AttributesBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.parser.ExtensibleParser;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;

class EditOperationStrategyProvider extends DomToNormalizedNodeParserFactory.BuildingStrategyProvider {

    private static final QName OPERATION_ATTRIBUTE = QName.create(EditConfigInput.QNAME.getNamespace(), null, XmlNetconfConstants.OPERATION_ATTR_KEY);

    private final DataTreeChangeTracker changeTracker;

    public EditOperationStrategyProvider(final DataTreeChangeTracker changeTracker) {
        this.changeTracker = changeTracker;
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, LeafNode<?>> forLeaf() {
        return new NetconfOperationLeafStrategy(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, ContainerNode> forContainer() {
        return new NetconfOperationContainerStrategy<>(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, MapNode> forMap() {
        return new NetconfOperationCollectionStrategy<>(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeWithValue, LeafSetEntryNode<?>> forLeafSetEntry() {
        return new NetconfOperationLeafSetEntryStrategy(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> forMapEntry() {
        return new NetconfOperationContainerStrategy<>(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, OrderedMapNode> forOrderedList() {
        return new NetconfOperationCollectionStrategy<>(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode> forUnkeyedListEntry() {
        return new NetconfOperationContainerStrategy<>(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, UnkeyedListNode> forUnkeyedList() {
        return new NetconfOperationCollectionStrategy<>(changeTracker);
    }

    @Override
    protected ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, ChoiceNode> forChoice() {
        return new NetconfOperationContainerStrategy<>(changeTracker);
    }

    @Override
    public ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.AugmentationIdentifier, AugmentationNode> forAugmentation() {
        return new NetconfOperationContainerStrategy<>(changeTracker);
    }

    private static class NetconfOperationCollectionStrategy<N extends NormalizedNode<YangInstanceIdentifier.NodeIdentifier, ?>> implements ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, N> {
        private final DataTreeChangeTracker changeTracker;

        public NetconfOperationCollectionStrategy(final DataTreeChangeTracker changeTracker) {
            this.changeTracker = changeTracker;
        }

        @Nullable
        @Override
        public N build(final NormalizedNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ?, N> builder) {
            changeTracker.popPath();
            return builder.build();
        }

        @Override
        public void prepareAttributes(final Map<QName, String> attributes, final NormalizedNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ?, N> containerBuilder) {
            changeTracker.pushPath(containerBuilder.build().getIdentifier());
        }
    }

    public static final class NetconfOperationLeafStrategy implements ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeIdentifier, LeafNode<?>> {

        private final DataTreeChangeTracker dataTreeChangeTracker;

        public NetconfOperationLeafStrategy(final DataTreeChangeTracker dataTreeChangeTracker) {
            this.dataTreeChangeTracker = dataTreeChangeTracker;
        }

        @Nullable
        @Override
        public LeafNode<?> build(final NormalizedNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ?, LeafNode<?>> builder) {
            LeafNode<?> node = builder.build();
            String operation = (String) node.getAttributeValue(OPERATION_ATTRIBUTE);
            if (operation == null) {
                return node;
            }

            if(builder instanceof AttributesBuilder<?>) {
                ((AttributesBuilder<?>) builder).withAttributes(Collections.<QName, String>emptyMap());
            }

            node = builder.build();

            ModifyAction action = ModifyAction.fromXmlValue(operation);
            if (dataTreeChangeTracker.getDeleteOperationTracker() > 0 || dataTreeChangeTracker.getRemoveOperationTracker() > 0) {
                return node;
            } else {
                if (!action.equals(dataTreeChangeTracker.peekAction())) {
                    dataTreeChangeTracker.pushPath(node.getIdentifier());
                    dataTreeChangeTracker.addDataTreeChange(new DataTreeChangeTracker.DataTreeChange(node, action, new ArrayList<>(dataTreeChangeTracker.getCurrentPath())));
                    dataTreeChangeTracker.popPath();
                    return null;
                } else {
                    return node;
                }
            }
        }

        @Override
        public void prepareAttributes(final Map<QName, String> attributes, final NormalizedNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ?, LeafNode<?>> containerBuilder) {
            // Noop
        }
    }

    public static final class NetconfOperationLeafSetEntryStrategy implements ExtensibleParser.BuildingStrategy<YangInstanceIdentifier.NodeWithValue, LeafSetEntryNode<?>> {

        private final DataTreeChangeTracker dataTreeChangeTracker;

        public NetconfOperationLeafSetEntryStrategy(final DataTreeChangeTracker dataTreeChangeTracker) {
            this.dataTreeChangeTracker = dataTreeChangeTracker;
        }

        @Nullable
        @Override
        public LeafSetEntryNode<?> build(final NormalizedNodeBuilder<YangInstanceIdentifier.NodeWithValue, ?, LeafSetEntryNode<?>> builder) {
            LeafSetEntryNode<?> node = builder.build();
            String operation = (String) node.getAttributeValue(OPERATION_ATTRIBUTE);
            if (operation == null) {
                return node;
            }

            if (builder instanceof AttributesBuilder<?>) {
                ((AttributesBuilder<?>) builder).withAttributes(Collections.<QName, String>emptyMap());
            }

            node = builder.build();

            ModifyAction action = ModifyAction.fromXmlValue(operation);
            if (dataTreeChangeTracker.getDeleteOperationTracker() > 0 || dataTreeChangeTracker.getRemoveOperationTracker() > 0) {
                return node;
            } else {
                if (!action.equals(dataTreeChangeTracker.peekAction())) {
                    dataTreeChangeTracker.pushPath(node.getIdentifier());
                    dataTreeChangeTracker.addDataTreeChange(new DataTreeChangeTracker.DataTreeChange(node, action, new ArrayList<>(dataTreeChangeTracker.getCurrentPath())));
                    dataTreeChangeTracker.popPath();
                    return null;
                } else {
                    return node;
                }
            }
        }

        @Override
        public void prepareAttributes(final Map<QName, String> attributes, final NormalizedNodeBuilder<YangInstanceIdentifier.NodeWithValue, ?, LeafSetEntryNode<?>> containerBuilder) {

        }
    }

    public static final class NetconfOperationContainerStrategy<P extends YangInstanceIdentifier.PathArgument, N extends DataContainerNode<P>> implements ExtensibleParser.BuildingStrategy<P, N> {

        private final DataTreeChangeTracker dataTreeChangeTracker;

        public NetconfOperationContainerStrategy(final DataTreeChangeTracker dataTreeChangeTracker) {
            this.dataTreeChangeTracker = dataTreeChangeTracker;
        }

        @Nullable
        @Override
        public N build(final NormalizedNodeBuilder<P, ?, N> builder) {
            if (builder instanceof AttributesBuilder<?>) {
                ((AttributesBuilder<?>) builder).withAttributes(Collections.<QName, String>emptyMap());
            }

            final N node = builder.build();
            final ModifyAction currentAction = dataTreeChangeTracker.popAction();

            //if we know that we are going to delete a parent node just complete the entire subtree
            if (dataTreeChangeTracker.getDeleteOperationTracker() > 0 || dataTreeChangeTracker.getRemoveOperationTracker() > 0) {
                dataTreeChangeTracker.popPath();
                return node;
            } else {
                //if parent and current actions dont match create a DataTreeChange and add it to the change list
                //dont add a new child to the parent node
                if (!currentAction.equals(dataTreeChangeTracker.peekAction())) {
                    dataTreeChangeTracker.addDataTreeChange(new DataTreeChangeTracker.DataTreeChange(node, currentAction, new ArrayList<>(dataTreeChangeTracker.getCurrentPath())));
                    dataTreeChangeTracker.popPath();
                    return null;
                } else {
                    dataTreeChangeTracker.popPath();
                    return node;
                }
            }
        }

        @Override
        public void prepareAttributes(final Map<QName, String> attributes, final NormalizedNodeBuilder<P, ?, N> containerBuilder) {
            dataTreeChangeTracker.pushPath(containerBuilder.build().getIdentifier());
            final String operation = attributes.get(OPERATION_ATTRIBUTE);
            if (operation != null) {
                dataTreeChangeTracker.pushAction(ModifyAction.fromXmlValue(operation));
            } else {
                dataTreeChangeTracker.pushAction(dataTreeChangeTracker.peekAction() != null
                        ? dataTreeChangeTracker.peekAction() : dataTreeChangeTracker.getDefaultAction());
            }
        }
    }
}
