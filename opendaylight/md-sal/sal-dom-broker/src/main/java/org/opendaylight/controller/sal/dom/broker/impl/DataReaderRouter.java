/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Iterables;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class DataReaderRouter extends AbstractDataReadRouter<YangInstanceIdentifier, CompositeNode> {
    private final static Logger LOG = LoggerFactory
            .getLogger(DataReaderRouter.class);
    private final static URI NETCONF_NAMESPACE = URI
            .create("urn:ietf:params:xml:ns:netconf:base:1.0");
    private final static QName NETCONF_DATA = new QName(NETCONF_NAMESPACE,
            "data");

    @Override
    protected CompositeNodeTOImpl merge(final YangInstanceIdentifier path,
            final Iterable<CompositeNode> data) {
        PathArgument pathArgument = Iterables.getLast(path.getPathArguments(), null);
        boolean empty = true;
        QName name = (pathArgument == null ? null : pathArgument.getNodeType());
        final ArrayList<Node<?>> nodes = new ArrayList<Node<?>>();
        final HashMap<QName, SimpleNode<?>> keyNodes = new HashMap<QName, SimpleNode<?>>();
        for (final CompositeNode dataBit : data) {
            try {
                if (pathArgument != null && dataBit != null) {
                    empty = false;
                    final Map<QName, SimpleNode<?>> keyNodesLocal = getKeyNodes(
                            pathArgument, dataBit);
                    nodes.addAll(this.childrenWithout(dataBit,
                            keyNodesLocal.entrySet()));
                } else if (dataBit != null) {
                    empty = false;
                    nodes.addAll(dataBit.getValue());
                }
            } catch (IllegalStateException e) {
                LOG.error("BUG: Readed data for path {} was invalid", path, e);
            }
        }
        if (empty) {
            return null;
        }
        /**
         * Reading from Root
         *
         */
        if (pathArgument == null) {
            return new CompositeNodeTOImpl(NETCONF_DATA, null, nodes);
        }
        final ArrayList<Node<?>> finalNodes = new ArrayList<Node<?>>(
                nodes.size() + keyNodes.size());
        finalNodes.addAll(keyNodes.values());
        finalNodes.addAll(nodes);
        return new CompositeNodeTOImpl(name, null, finalNodes);
    }

    protected Map<QName, SimpleNode<?>> _getKeyNodes(
            final PathArgument argument, final CompositeNode node) {
        return Collections.emptyMap();
    }

    protected Map<QName, SimpleNode<?>> _getKeyNodes(
            final NodeIdentifierWithPredicates argument,
            final CompositeNode node) {
        final HashMap<QName, SimpleNode<?>> ret = new HashMap<QName, SimpleNode<?>>();
        for (final Entry<QName, Object> keyValue : argument.getKeyValues()
                .entrySet()) {
            final List<SimpleNode<?>> simpleNode = node
                    .getSimpleNodesByName(keyValue.getKey());
            if (simpleNode != null && !simpleNode.isEmpty()) {
                checkState(
                        simpleNode.size() <= 1,
                        "Only one simple node for key $s is allowed in node $s",
                        keyValue.getKey(), node);
                checkState(
                        simpleNode.get(0).getValue().equals(keyValue.getValue()),
                        "Key node must equal to instance identifier value in node $s",
                        node);
                ret.put(keyValue.getKey(), simpleNode.get(0));
            }
            final List<CompositeNode> compositeNode = node
                    .getCompositesByName(keyValue.getKey());
            checkState(compositeNode == null || compositeNode.isEmpty(),
                    "Key node must be Simple Node, not composite node.");
        }
        return ret;
    }

    public Map<QName, SimpleNode<?>> getKeyNodes(
            final YangInstanceIdentifier.PathArgument argument,
            final CompositeNode node) {
        if (argument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            return _getKeyNodes(
                    (YangInstanceIdentifier.NodeIdentifierWithPredicates) argument,
                    node);
        } else if (argument != null) {
            return _getKeyNodes(argument, node);
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: "
                    + Arrays.<Object> asList(argument, node).toString());
        }
    }

    private Collection<? extends Node<?>> childrenWithout(
            final CompositeNode node,
            final Set<Entry<QName, SimpleNode<?>>> entries) {
        if (entries.isEmpty()) {
            return node.getValue();
        }
        final List<Node<?>> filteredNodes = new ArrayList<Node<?>>();
        for (final Node<?> scannedNode : node.getValue()) {
            if (!entries.contains(scannedNode.getNodeType())) {
                filteredNodes.add(scannedNode);
            }
        }
        return filteredNodes;
    }

}
