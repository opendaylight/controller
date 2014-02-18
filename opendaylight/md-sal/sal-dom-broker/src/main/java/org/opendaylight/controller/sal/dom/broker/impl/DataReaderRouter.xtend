/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl

import java.net.URI
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.Map
import java.util.Map.Entry
import java.util.Set
import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.api.SimpleNode
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.slf4j.LoggerFactory

import static com.google.common.base.Preconditions.*

class DataReaderRouter extends AbstractDataReadRouter<InstanceIdentifier, CompositeNode> {
    private static val LOG = LoggerFactory.getLogger(DataReaderRouter);
    private static val NETCONF_NAMESPACE = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0")
    private static val NETCONF_DATA = new QName(NETCONF_NAMESPACE,"data");

    override protected merge(InstanceIdentifier path, Iterable<CompositeNode> data) {
        val pathArgument = path.path.last;
        var empty = true;
        var name = pathArgument?.nodeType;
        val nodes = new ArrayList<Node<?>>();
        val keyNodes = new HashMap<QName, SimpleNode<?>>();
        for(dataBit : data) {
            try {
                if(pathArgument != null && dataBit != null) {
                    empty = false;
                    val keyNodesLocal = getKeyNodes(pathArgument,dataBit);
                    nodes.addAll(dataBit.childrenWithout(keyNodesLocal.entrySet));
                } else if (dataBit != null) {
                    empty = false;
                    nodes.addAll(dataBit.children)
                }
            }   catch (IllegalStateException e) {
                LOG.error("BUG: Readed data for path {} was invalid",path,e);
            }
        }
        if(empty) {
            return null;
        }
        /**
         * Reading from Root
         * 
         */
        if(pathArgument == null) {
            return new CompositeNodeTOImpl(NETCONF_DATA,null,nodes);
        }
        val finalNodes = new ArrayList<Node<?>>();
        finalNodes.addAll(keyNodes.values);
        finalNodes.addAll(nodes);
        return new CompositeNodeTOImpl(name,null,finalNodes);
    }
    
    
    
    dispatch def Map<QName, SimpleNode<?>> getKeyNodes(PathArgument argument, CompositeNode node) {
        return Collections.emptyMap();
    }
    
    dispatch def getKeyNodes(NodeIdentifierWithPredicates argument, CompositeNode node) {
        val ret = new HashMap<QName, SimpleNode<?>>();
        for (keyValue : argument.keyValues.entrySet) {
            val simpleNode = node.getSimpleNodesByName(keyValue.key);
            if(simpleNode !== null && !simpleNode.empty) {
                checkState(simpleNode.size <= 1,"Only one simple node for key $s is allowed in node $s",keyValue.key,node);
                checkState(simpleNode.get(0).value == keyValue.value,"Key node must equal to instance identifier value in node $s",node);
                ret.put(keyValue.key,simpleNode.get(0));
            }
            val compositeNode = node.getCompositesByName(keyValue.key);
            checkState(compositeNode === null || compositeNode.empty,"Key node must be Simple Node, not composite node.");
        }
        return ret;
    }
    
    def Collection<? extends Node<?>> childrenWithout(CompositeNode node, Set<Entry<QName, SimpleNode<?>>> entries) {
        if(entries.empty) {
            return node.children;
        }
        val filteredNodes = new ArrayList<Node<?>>();
        for(scannedNode : node.children) {
            if(!entries.contains(scannedNode.nodeType)) {
                filteredNodes.add(scannedNode);
            }
        }
        return filteredNodes;
    }
    
}
