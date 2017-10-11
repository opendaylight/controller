/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.MustDefinition;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

public final class YangSchemaUtils {
    private YangSchemaUtils() {
        throw new UnsupportedOperationException("Utility class.");
    }

    public static DataSchemaNode getSchemaNode(final SchemaContext schema, final YangInstanceIdentifier path) {
        checkArgument(schema != null, "YANG Schema must not be null.");
        checkArgument(path != null, "Path must not be null.");
        return getSchemaNode(schema, Lists.transform(path.getPathArguments(), PathArgument::getNodeType));
    }

    public static DataSchemaNode getSchemaNode(final SchemaContext schema, final Iterable<QName> path) {
        checkArgument(schema != null, "YANG Schema must not be null.");
        checkArgument(path != null, "Path must not be null.");
        if (!path.iterator().hasNext()) {
            return toRootDataNode(schema);
        }

        QName firstNode = path.iterator().next();
        DataNodeContainer previous = schema.findModule(firstNode.getModule()).orElse(null);
        Iterator<QName> iterator = path.iterator();

        while (iterator.hasNext()) {
            checkArgument(previous != null, "Supplied path does not resolve into valid schema node.");
            QName arg = iterator.next();
            DataSchemaNode currentNode = previous.getDataChildByName(arg);
            if (currentNode == null) {
                currentNode = searchInChoices(previous, arg);
            }
            if (currentNode instanceof DataNodeContainer) {
                previous = (DataNodeContainer) currentNode;
            } else if (currentNode instanceof LeafSchemaNode || currentNode instanceof LeafListSchemaNode) {
                checkArgument(!iterator.hasNext(), "Path nests inside leaf node, which is not allowed.");
                return currentNode;
            }
            checkState(currentNode != null, "Current node should not be null for %s", path);
        }
        checkState(previous instanceof DataSchemaNode,
            "Schema node for %s should be instance of DataSchemaNode. Found %s", path, previous);
        return (DataSchemaNode) previous;
    }

    private static DataSchemaNode searchInChoices(final DataNodeContainer node, final QName arg) {
        for (DataSchemaNode child : node.getChildNodes()) {
            if (child instanceof ChoiceSchemaNode) {
                ChoiceSchemaNode choiceNode = (ChoiceSchemaNode) child;
                DataSchemaNode potential = searchInCases(choiceNode, arg);
                if (potential != null) {
                    return potential;
                }
            }
        }
        return null;
    }

    private static DataSchemaNode searchInCases(final ChoiceSchemaNode choiceNode, final QName arg) {
        for (CaseSchemaNode caseNode : choiceNode.getCases().values()) {
            DataSchemaNode node = caseNode.getDataChildByName(arg);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private static ContainerSchemaNode toRootDataNode(final SchemaContext schema) {
        return new NetconfDataRootNode(schema);
    }

    private static final class NetconfDataRootNode implements ContainerSchemaNode {

        NetconfDataRootNode(final SchemaContext schema) {
            // TODO Auto-generated constructor stub
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<DataSchemaNode> getChildNodes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Optional<DataSchemaNode> findDataChildByName(final QName name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<UsesNode> getUses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<AugmentationSchemaNode> getAvailableAugmentations() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isAugmenting() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isAddedByUses() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isConfiguration() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public QName getQName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SchemaPath getPath() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getReference() {
            return Optional.empty();
        }

        @Override
        public Status getStatus() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isPresenceContainer() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Set<NotificationDefinition> getNotifications() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<ActionDefinition> getActions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Optional<RevisionAwareXPath> getWhenCondition() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<MustDefinition> getMustConstraints() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
