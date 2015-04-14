/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditConfigInput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.AttributesContainer;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.parser.LeafStrategy;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.parser.ParsingStrategy;

public class DataTreeChangeTracker {

    private static final QName OPERATION_ATTRIBUTE = QName.create(EditConfigInput.QNAME.getNamespace(), null, XmlNetconfConstants.OPERATION_ATTR_KEY);

    private final ModifyAction defaultAction;

    private final Deque<ModifyAction> actions;
    private final Deque<PathArgument> currentPath;
    private final ArrayList<DataTreeChange> dataTreeChanges;
    private int deleteOperationTracker = 0;
    private int removeOperationTracker = 0;

    public DataTreeChangeTracker(final ModifyAction defaultAction) {
        this.defaultAction = defaultAction;
        this.currentPath = new ArrayDeque<>();
        this.actions = new ArrayDeque<>();
        this.dataTreeChanges = new ArrayList<>();
    }

    public void pushAction(final ModifyAction action) {
        if (ModifyAction.DELETE.equals(action)) {
            deleteOperationTracker++;
        }

        if (ModifyAction.REMOVE.equals(action)) {
            removeOperationTracker++;
        }
        this.actions.push(action);
    }

    public ModifyAction peekAction() {
        return this.actions.peekFirst();
    }

    public ModifyAction popAction() {
        final ModifyAction popResult = actions.pop();
        if (ModifyAction.DELETE.equals(popResult)) {
            deleteOperationTracker--;
        }

        if (ModifyAction.REMOVE.equals(popResult)) {
            removeOperationTracker--;
        }
        return popResult;
    }

    public int getDeleteOperationTracker() {
        return deleteOperationTracker;
    }

    public int getRemoveOperationTracker() {
        return removeOperationTracker;
    }

    public void addDataTreeChange(final DataTreeChange change) {
        dataTreeChanges.add(change);
    }

    public ArrayList<DataTreeChange> getDataTreeChanges() {
        return dataTreeChanges;
    }

    public ModifyAction getDefaultAction() {
        return defaultAction;
    }

    public void pushPath(final PathArgument pathArgument) {
        currentPath.push(pathArgument);
    }

    public PathArgument popPath() {
        return currentPath.pop();
    }

    public Deque<PathArgument> getCurrentPath() {
        return currentPath;
    }

    public static final class NetconfOperationLeafStrategy implements LeafStrategy {

        private final DataTreeChangeTracker dataTreeChangeTracker;

        public NetconfOperationLeafStrategy(final DataTreeChangeTracker dataTreeChangeTracker) {
            this.dataTreeChangeTracker = dataTreeChangeTracker;
        }

        @Override
        public NormalizedNode applyStrategy(final NormalizedNodeAttrBuilder builder) {
            NormalizedNode node = builder.build();
            if (node instanceof AttributesContainer) {
                String operation = (String) ((AttributesContainer) node).getAttributeValue(OPERATION_ATTRIBUTE);
                if (operation != null) {
                    node = ((NormalizedNodeAttrBuilder) builder.withAttributes(Collections.emptyMap())).build();
                    ModifyAction action = ModifyAction.fromXmlValue(operation);
                    if (dataTreeChangeTracker.getDeleteOperationTracker() > 0 || dataTreeChangeTracker.getRemoveOperationTracker() > 0) {
                        return node;
                    } else {
                        if (!action.equals(dataTreeChangeTracker.peekAction())) {
                            dataTreeChangeTracker.pushPath(node.getIdentifier());
                            dataTreeChangeTracker.addDataTreeChange(new DataTreeChange(node, action, new ArrayList<>(dataTreeChangeTracker.getCurrentPath())));
                            dataTreeChangeTracker.popPath();
                            return null;
                        } else {
                            return node;
                        }
                    }
                }
            }
            return node;
        }
    }

    public static final class NetconfOperationContainerStrategy implements ParsingStrategy {

        private final DataTreeChangeTracker dataTreeChangeTracker;

        public NetconfOperationContainerStrategy(final DataTreeChangeTracker dataTreeChangeTracker) {
            this.dataTreeChangeTracker = dataTreeChangeTracker;
        }

        @Override
        public DataContainerNode<?> applyStrategy(final DataContainerNodeBuilder containerBuilder) {
            if (containerBuilder instanceof DataContainerNodeAttrBuilder) {
                ((DataContainerNodeAttrBuilder) containerBuilder).withAttributes(Collections.emptyMap());
            }

            final DataContainerNode node = (DataContainerNode) containerBuilder.build();
            final ModifyAction currentAction = dataTreeChangeTracker.popAction();

            //if we know that we are going to delete a parent node just complete the entire subtree
            if (dataTreeChangeTracker.getDeleteOperationTracker() > 0 || dataTreeChangeTracker.getRemoveOperationTracker() > 0) {
                dataTreeChangeTracker.popPath();
                return node;
            } else {
                //if parent and current actions dont match create a DataTreeChange and add it to the change list
                //dont add a new child to the parent node
                if (!currentAction.equals(dataTreeChangeTracker.peekAction())) {
                    dataTreeChangeTracker.addDataTreeChange(new DataTreeChange(node, currentAction, new ArrayList<>(dataTreeChangeTracker.getCurrentPath())));
                    dataTreeChangeTracker.popPath();
                    return null;
                } else {
                    dataTreeChangeTracker.popPath();
                    return node;
                }
            }
        }

        @Override
        public void prepareAttribues(final Map attributes, final DataContainerNodeBuilder containerBuilder) {
            dataTreeChangeTracker.pushPath(containerBuilder.build().getIdentifier());
            final String operation = ((Map<QName, String>) attributes).get(OPERATION_ATTRIBUTE);
            if (operation != null) {
                dataTreeChangeTracker.pushAction(ModifyAction.fromXmlValue(operation));
            } else {
                dataTreeChangeTracker.pushAction(dataTreeChangeTracker.peekAction() != null
                        ? dataTreeChangeTracker.peekAction() : dataTreeChangeTracker.getDefaultAction());
            }
        }

        @Override
        public void addListIdentifier(final QName listIdent) {
            dataTreeChangeTracker.pushPath(new NodeIdentifier(listIdent));
        }

        @Override
        public void popListIdentifier() {
            dataTreeChangeTracker.popPath();
        }
    }

    public static final class DataTreeChange {

        private final NormalizedNode changeRoot;
        private final ModifyAction action;
        private final List<PathArgument> path;

        public DataTreeChange(final NormalizedNode changeRoot, final ModifyAction action, final ArrayList<PathArgument> path) {
            this.changeRoot = changeRoot;
            this.action = action;
            this.path = Lists.reverse(path);
        }

        public NormalizedNode getChangeRoot() {
            return changeRoot;
        }

        public ModifyAction getAction() {
            return action;
        }

        public List<PathArgument> getPath() {
            return path;
        }
    }
}
