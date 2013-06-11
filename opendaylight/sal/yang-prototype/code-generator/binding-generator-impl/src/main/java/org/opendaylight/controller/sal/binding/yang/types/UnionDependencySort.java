/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.parser.util.TopologicalSort;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.Node;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnionDependencySort {
    private static final Logger logger = LoggerFactory
            .getLogger(UnionDependencySort.class);

    public static List<ExtendedType> sort(
            final Set<TypeDefinition<?>> typeDefinitions) {
        if (typeDefinitions == null) {
            logger.error("Set of Type Definitions cannot be NULL!");
            throw new IllegalArgumentException("Set of Type Definitions " +
                    "cannot be NULL!");
        }

        final Set<ExtendedType> extUnionTypes =
                unionsFromTypeDefinitions(typeDefinitions);

        final Set<Node> unsorted = unionTypesToUnionNodes(extUnionTypes);

        final List<Node> sortedNodes = TopologicalSort.sort(unsorted);
        return Lists.transform(sortedNodes, new Function<Node, ExtendedType>() {
            @Override
            public ExtendedType apply(Node input) {
                return ((UnionNode) input).getUnionType();
            }
        });
    }

    private static Set<ExtendedType> unionsFromTypeDefinitions(
            final Set<TypeDefinition<?>> typeDefinitions) {
        final Set<ExtendedType> unions = Sets.newHashSet();

        for (final TypeDefinition<?> typedef : typeDefinitions) {
            if ((typedef != null) && (typedef.getBaseType() != null)) {
                if (typedef instanceof ExtendedType
                        && typedef.getBaseType() instanceof UnionTypeDefinition) {
                    unions.add((ExtendedType) typedef);
                }
            }
        }
        return unions;
    }

    private static Set<Node> unionTypesToUnionNodes(
            final Set<ExtendedType> extUnionTypes) {
        final Map<ExtendedType, Node> nodeMap = Maps.newHashMap();
        final Set<Node> resultNodes = Sets.newHashSet();

        for (final ExtendedType unionType : extUnionTypes) {
            final Node node = new UnionNode(unionType);
            nodeMap.put(unionType, node);
            resultNodes.add(node);
        }

        for (final Node node : resultNodes) {
            final UnionNode unionNode = (UnionNode) node;
            final ExtendedType extUnionType = unionNode.getUnionType();

            final UnionTypeDefinition unionType = (UnionTypeDefinition)
                    extUnionType.getBaseType();

            final List<TypeDefinition<?>> innerTypes = unionType.getTypes();
            for (final TypeDefinition<?> typedef : innerTypes) {
                if (extUnionTypes.contains(typedef)) {
                    final Node toNode = nodeMap.get(typedef);
                    unionNode.addEdge(toNode);
                }
            }
        }

        return resultNodes;
    }

    private static UnionNode unionTypeToUnionNode(
            final ExtendedType extUnionType,
            final Set<ExtendedType> extUnionTypes) {
        final UnionNode node = new UnionNode(extUnionType);

        if (extUnionType.getBaseType() instanceof UnionTypeDefinition) {
            final UnionTypeDefinition unionType = (UnionTypeDefinition)
                    extUnionType.getBaseType();

            final List<TypeDefinition<?>> innerTypes = unionType.getTypes();
            for (final TypeDefinition<?> typedef : innerTypes) {
                if ((typedef != null) && (typedef instanceof ExtendedType)
                        && (typedef.getBaseType() instanceof UnionTypeDefinition)) {
                    if (extUnionTypes.contains(typedef)) {
                        node.addEdge(new UnionNode((ExtendedType) typedef));
                    }
                }
            }
        }

        return node;
    }

    @VisibleForTesting
    static final class UnionNode extends NodeImpl {
        private final ExtendedType unionType;

        UnionNode(ExtendedType unionType) {
            this.unionType = unionType;
        }

        ExtendedType getUnionType() {
            return unionType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UnionNode)) {
                return false;
            }
            UnionNode unionNode = (UnionNode) o;
            if (!unionType.equals(unionNode.unionType)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return unionType.hashCode();
        }

        @Override
        public String toString() {
            return "UnionNode{" +
                    "unionType=" + unionType +
                    '}';
        }
    }
}
