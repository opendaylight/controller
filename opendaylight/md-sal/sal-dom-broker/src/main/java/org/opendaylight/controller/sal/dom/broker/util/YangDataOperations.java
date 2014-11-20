/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

@Deprecated
public class YangDataOperations {

    public static CompositeNode merge(final DataSchemaNode schema,
            final CompositeNode stored, final CompositeNode modified,
            final boolean config) {
        if (stored == null) {
            return modified;
        }

        Preconditions.checkArgument(schema instanceof ListSchemaNode
                || schema instanceof ContainerSchemaNode,
                "Supplied node is not data node container.");

        return YangDataOperations.mergeContainer((DataNodeContainer) schema,
                stored, modified, config);
    }

    private static Iterable<? extends Node<?>> _mergeMultiple(
            final LeafSchemaNode node, final List<Node<?>> original,
            final List<Node<?>> modified, final boolean configurational) {
        checkArgument(original.size() == 1);
        checkArgument(modified.size() == 1);

        return modified;
    }

    private static Iterable<? extends Node<?>> _mergeMultiple(
            final LeafListSchemaNode node, final List<Node<?>> original,
            final List<Node<?>> modified, final boolean configurational) {
        return modified;
    }

    private static Iterable<? extends Node<?>> _mergeMultiple(
            final ContainerSchemaNode node, final List<Node<?>> original,
            final List<Node<?>> modified, final boolean configurational) {
        checkArgument(original.size() == 1);
        checkArgument(modified.size() == 1);
        return Collections.singletonList(merge(node,
                (CompositeNode) original.get(0),
                (CompositeNode) modified.get(0), configurational));
    }

    private static Iterable<? extends Node<?>> _mergeMultiple(
            final ListSchemaNode node, final List<Node<?>> original,
            final List<Node<?>> modified, final boolean configurational) {

        if (node.getKeyDefinition() == null
                || node.getKeyDefinition().isEmpty()) {
            return modified;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Map<Map<QName, Object>, CompositeNode> originalMap = YangDataUtils
        .toIndexMap((List) original, node.getKeyDefinition());
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Map<Map<QName, Object>, CompositeNode> modifiedMap = YangDataUtils
        .toIndexMap((List) modified, node.getKeyDefinition());

        final List<Node<?>> mergedNodes = new ArrayList<Node<?>>(
                original.size() + modified.size());
        for (final Map.Entry<Map<QName, Object>, CompositeNode> entry : modifiedMap
                .entrySet()) {
            final CompositeNode originalEntry = originalMap.get(entry.getKey());
            if (originalEntry != null) {
                originalMap.remove(entry.getKey());
                mergedNodes.add(merge(node, originalEntry, entry.getValue(),
                        configurational));
            } else {
                mergedNodes.add(entry.getValue());
            }
        }
        mergedNodes.addAll(originalMap.values());
        return mergedNodes;
    }

    private static Iterable<? extends Node<?>> mergeMultiple(
            final DataSchemaNode node, final List<Node<?>> original,
            final List<Node<?>> modified, final boolean configurational) {
        if (node instanceof ContainerSchemaNode) {
            return _mergeMultiple((ContainerSchemaNode) node, original,
                    modified, configurational);
        } else if (node instanceof LeafListSchemaNode) {
            return _mergeMultiple((LeafListSchemaNode) node, original,
                    modified, configurational);
        } else if (node instanceof LeafSchemaNode) {
            return _mergeMultiple((LeafSchemaNode) node, original, modified,
                    configurational);
        } else if (node instanceof ListSchemaNode) {
            return _mergeMultiple((ListSchemaNode) node, original, modified,
                    configurational);
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: "
                    + Arrays.<Object> asList(node, original, modified,
                            configurational).toString());
        }
    }

    private static CompositeNode mergeContainer(final DataNodeContainer schema,
            final CompositeNode stored, final CompositeNode modified,
            final boolean config) {
        if (stored == null) {
            return modified;
        }
        Preconditions.checkNotNull(stored);
        Preconditions.checkNotNull(modified);
        Preconditions.checkArgument(Objects.equals(stored.getNodeType(),
                modified.getNodeType()));

        final List<Node<?>> mergedChildNodes = new ArrayList<Node<?>>(stored
                .getValue().size() + modified.getValue().size());
        final Set<QName> toProcess = new HashSet<QName>(stored.keySet());
        toProcess.addAll(modified.keySet());

        for (QName qname : toProcess) {
            final DataSchemaNode schemaChild = schema.getDataChildByName(qname);
            final List<Node<?>> storedChildren = stored.get(qname);
            final List<Node<?>> modifiedChildren = modified.get(qname);

            if (modifiedChildren != null && !modifiedChildren.isEmpty()) {
                if (storedChildren == null || storedChildren.isEmpty()
                        || schemaChild == null) {
                    mergedChildNodes.addAll(modifiedChildren);
                } else {
                    final Iterable<? extends Node<?>> _mergeMultiple = mergeMultiple(
                            schemaChild, storedChildren, modifiedChildren,
                            config);
                    Iterables.addAll(mergedChildNodes, _mergeMultiple);
                }
            } else if (storedChildren != null && !storedChildren.isEmpty()) {
                mergedChildNodes.addAll(storedChildren);
            }
        }
        return new CompositeNodeTOImpl(stored.getNodeType(), null,
                mergedChildNodes);
    }
}
