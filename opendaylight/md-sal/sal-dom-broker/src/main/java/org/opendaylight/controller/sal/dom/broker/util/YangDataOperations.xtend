/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util

import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import static com.google.common.base.Preconditions.*;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import java.util.ArrayList

import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import java.util.List
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode
import java.util.Collections
import java.util.HashSet
import org.opendaylight.yangtools.yang.common.QName
import static extension org.opendaylight.controller.sal.dom.broker.util.YangDataUtils.*;

class YangDataOperations {

    static def CompositeNode merge(DataSchemaNode schema, CompositeNode stored, CompositeNode modified, boolean config) {
        if (stored === null) {
            return modified;
        }

        if (schema instanceof ListSchemaNode || schema instanceof ContainerSchemaNode) {
            return mergeContainer(schema as DataNodeContainer, stored, modified, config);
        }
        throw new IllegalArgumentException("Supplied node is not data node container.");
    }

    private static dispatch def Iterable<? extends Node<?>> mergeMultiple(LeafSchemaNode node, List<Node<?>> original,
        List<Node<?>> modified, boolean configurational) {
        checkArgument(original.size === 1);
        checkArgument(modified.size === 1);
        
        return modified;
    }

    private static dispatch def Iterable<? extends Node<?>> mergeMultiple(LeafListSchemaNode node,
        List<Node<?>> original, List<Node<?>> modified, boolean configurational) {
        return modified;
    }

    private static dispatch def Iterable<? extends Node<?>> mergeMultiple(ContainerSchemaNode node,
        List<Node<?>> original, List<Node<?>> modified, boolean configurational) {
        checkArgument(original.size === 1);
        checkArgument(modified.size === 1);
        return Collections.singletonList(
            merge(node, original.get(0) as CompositeNode, modified.get(0) as CompositeNode, configurational));
    }

    private static dispatch def Iterable<? extends Node<?>> mergeMultiple(ListSchemaNode node, List<Node<?>> original,
        List<Node<?>> modified, boolean configurational) {
        
        if(node.keyDefinition === null || node.keyDefinition.empty) {
            return modified;
        }
        val originalMap = (original as List).toIndexMap(node.keyDefinition);
        val modifiedMap = (modified as List).toIndexMap(node.keyDefinition);
        
        val List<Node<?>> mergedNodes = new ArrayList(original.size + modified.size);
        for(entry : modifiedMap.entrySet) {
            val originalEntry = originalMap.get(entry.key);
            if(originalEntry != null) {
                originalMap.remove(entry.key);
                mergedNodes.add(merge(node,originalEntry,entry.value,configurational));
            } else {
                mergedNodes.add(entry.value);
            }
        }
        mergedNodes.addAll(originalMap.values);
        return mergedNodes;
    }

    static private def CompositeNode mergeContainer(DataNodeContainer schema, CompositeNode stored,
        CompositeNode modified, boolean config) {
        if (stored == null) {
            return modified;
        }
        checkNotNull(stored)
        checkNotNull(modified)
        checkArgument(stored.nodeType == modified.nodeType);

        val mergedChildNodes = new ArrayList<Node<?>>(stored.children.size + modified.children.size);
        
        val toProcess = new HashSet<QName>(stored.keySet);
        toProcess.addAll(modified.keySet);
        
        for (qname : toProcess) {
            val schemaChild = schema.getDataChildByName(qname);
            val storedChildren = stored.get(qname);
            val modifiedChildren = modified.get(qname);

            if (modifiedChildren !== null && !modifiedChildren.empty) {
                if (storedChildren === null || storedChildren.empty || schemaChild === null) {
                    mergedChildNodes.addAll(modifiedChildren);
                } else {
                    mergedChildNodes.addAll(mergeMultiple(schemaChild, storedChildren, modifiedChildren, config));
                }
            } else if (storedChildren !== null && !storedChildren.empty) {
                mergedChildNodes.addAll(storedChildren);
            }
        }
        return new CompositeNodeTOImpl(stored.nodeType, null, mergedChildNodes);
    }

}
