/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.parser.util.TopologicalSort;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.Node;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

//import org.opendaylight.controller.yang.model.util.GroupingDefinition;

public class GroupingDefinitionDependencySort {
    private static final Logger logger = LoggerFactory.getLogger(GroupingDefinitionDependencySort.class);

    public static List<GroupingDefinition> sort(final Set<GroupingDefinition> groupingDefinitions) {
        if (groupingDefinitions == null) {
            logger.error("Set of grouping definitions cannot be NULL!");
            throw new IllegalArgumentException("Set of Type Definitions " + "cannot be NULL!");
        }

        final List<GroupingDefinition> resultGroupingDefinitions = new ArrayList<GroupingDefinition>();
        final Set<Node> unsorted = groupingDefinitionsToGroupingNodes(groupingDefinitions);
        final List<Node> sortedNodes = TopologicalSort.sort(unsorted);
        for (Node node : sortedNodes) {
            resultGroupingDefinitions.add(((GroupingNode) node).getGroupingDefinition());
        }
        return resultGroupingDefinitions;

    }

    private static Set<Node> groupingDefinitionsToGroupingNodes(final Set<GroupingDefinition> groupingDefinitions) {
        final Map<SchemaPath, Node> nodeMap = Maps.newHashMap();
        final Set<Node> resultNodes = Sets.newHashSet();

        for (final GroupingDefinition groupingDefinition : groupingDefinitions) {
            final Node node = new GroupingNode(groupingDefinition);
            nodeMap.put(groupingDefinition.getPath(), node);
            resultNodes.add(node);
        }

        for (final Node node : resultNodes) {
            final GroupingNode groupingNode = (GroupingNode) node;
            final GroupingDefinition groupingDefinition = groupingNode.getGroupingDefinition();

            Set<UsesNode> usesNodes = groupingDefinition.getUses();
            for (UsesNode usesNode : usesNodes) {
                SchemaPath schemaPath = usesNode.getGroupingPath();
                if (schemaPath != null) {
                    Node nodeTo = nodeMap.get(schemaPath);
                    groupingNode.addEdge(nodeTo);
                }
            }
        }

        return resultNodes;
    }

    private static final class GroupingNode extends NodeImpl {
        private final GroupingDefinition groupingDefinition;

        GroupingNode(GroupingDefinition groupingDefinition) {
            this.groupingDefinition = groupingDefinition;
        }

        GroupingDefinition getGroupingDefinition() {
            return groupingDefinition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GroupingNode)) {
                return false;
            }
            GroupingNode groupingNode = (GroupingNode) o;
            if (!groupingDefinition.equals(groupingNode.groupingDefinition)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return groupingDefinition.hashCode();
        }

        @Override
        public String toString() {
            return "GroupingNode{" + "groupingType=" + groupingDefinition + '}';
        }
    }
}
