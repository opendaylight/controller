/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;
import org.opendaylight.controller.yang.data.api.SimpleNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;


/**
 * @author michal.rehak
 *
 */
public abstract class NodeUtils {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeUtils.class);
    
    /**
     * 
     */
    private static final String USER_KEY_NODE = "node";
    
    /**
     * @param node
     * @return node path up till root node
     */
    public static String buildPath(Node<?> node) {
        Vector<String> breadCrumbs = new Vector<>();
        Node<?> tmpNode = node;
        while (tmpNode != null) {
            breadCrumbs.insertElementAt(tmpNode.getNodeType().getLocalName(), 0);
            tmpNode = tmpNode.getParent();
        }
        
        return Joiner.on(".").join(breadCrumbs);
    }

    
    /**
     * @param treeRootNode
     * @return dom tree, containing same node structure, yang nodes are associated 
     * to dom nodes as user data
     */
    public static org.w3c.dom.Document buildShadowDomTree(CompositeNode treeRootNode) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document doc = null;
        try {
            DocumentBuilder bob = dbf.newDocumentBuilder();
            doc = bob.newDocument();
        } catch (ParserConfigurationException e) {
            LOG.error("documentBuilder problem", e);
            return null;
        }
        
        
        Stack<SimpleEntry<org.w3c.dom.Node, Node<?>>> jobQueue = new Stack<>();
        jobQueue.push(new SimpleEntry<org.w3c.dom.Node, Node<?>>(doc, treeRootNode));
        
        while (!jobQueue.isEmpty()) {
            SimpleEntry<org.w3c.dom.Node, Node<?>> job = jobQueue.pop();
            org.w3c.dom.Node jointPlace = job.getKey();
            Node<?> item = job.getValue();
            QName nodeType = item.getNodeType();
            Element itemEl = doc.createElementNS(nodeType.getNamespace().toString(), 
                    item.getNodeType().getLocalName());
            itemEl.setUserData(USER_KEY_NODE, item, null);
            if (item instanceof SimpleNode<?>) {
                Object value = ((SimpleNode<?>) item).getValue();
                itemEl.setTextContent(String.valueOf(value));
                //itemEl.setAttribute("type", value.getClass().getSimpleName());
            }
            if (item instanceof NodeModification) {
                ModifyAction modificationAction = ((NodeModification) item).getModificationAction();
                if (modificationAction != null) {
                    itemEl.setAttribute("modifyAction", modificationAction.toString());
                }
            }
            
            jointPlace.appendChild(itemEl);
            
            if (item instanceof CompositeNode) {
                for (Node<?> child : ((CompositeNode) item).getChildren()) {
                    jobQueue.push(new SimpleEntry<org.w3c.dom.Node, Node<?>>(itemEl, child));
                }
            }
        }
        
        return doc;
    }
    
    /**
     * @param doc
     * @param xpathEx
     * @return user data value on found node
     * @throws XPathExpressionException
     */
    @SuppressWarnings("unchecked")
    public static <T> T findNodeByXpath(org.w3c.dom.Document doc, String xpathEx) 
            throws XPathExpressionException {
        T userNode = null;
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathEx);
        
        org.w3c.dom.Node result = (org.w3c.dom.Node) expr.evaluate(doc, XPathConstants.NODE);
        if (result != null) {
            userNode = (T) result.getUserData(USER_KEY_NODE);
        } 
        
        return userNode;
    }


    /**
     * build NodeMap, where key = qName; value = node
     * 
     * @param value
     * @return map of children, where key = qName and value is list of children groupped by qName  
     */
    public static Map<QName, List<Node<?>>> buildNodeMap(List<Node<?>> value) {
        Map<QName, List<Node<?>>> nodeMapTmp = new HashMap<>();
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(
                    "nodeList should not be null or empty");
        }
        for (Node<?> node : value) {
            List<Node<?>> qList = nodeMapTmp.get(node.getNodeType());
            if (qList == null) {
                qList = new ArrayList<>();
                nodeMapTmp.put(node.getNodeType(), qList);
            }
            qList.add(node);
        }
        return nodeMapTmp;
    }


    /**
     * @param context
     * @return map of lists, where key = path; value = {@link DataSchemaNode}
     */
    public static Map<String, ListSchemaNode> buildMapOfListNodes(
            SchemaContext context) {
        Map<String, ListSchemaNode> mapOfLists = new HashMap<>();
        
        Stack<DataSchemaNode> jobQueue = new Stack<>();
        jobQueue.addAll(context.getDataDefinitions());
        
        while (!jobQueue.isEmpty()) {
            DataSchemaNode dataSchema = jobQueue.pop();
            if (dataSchema instanceof ListSchemaNode) {
                mapOfLists.put(schemaPathToPath(dataSchema.getPath().getPath()), (ListSchemaNode) dataSchema);
            }
            
            if (dataSchema instanceof DataNodeContainer) {
                jobQueue.addAll(((DataNodeContainer) dataSchema).getChildNodes());
            }
        }
        
        return mapOfLists;
    }
    
    /**
     * @param path
     * @return
     */
    private static String schemaPathToPath(List<QName> qNamesPath) {
        List<String> pathSeed = new ArrayList<>();
        for (QName qNameItem : qNamesPath) {
            pathSeed.add(qNameItem.getLocalName());
        }
        return Joiner.on(".").join(pathSeed);
    }

    /**
     * add given node to it's parent's list of children
     * @param newNode
     */
    public static void fixParentRelation(Node<?> newNode) {
        if (newNode.getParent() != null) {
            List<Node<?>> siblings = newNode.getParent().getChildren();
            if (!siblings.contains(newNode)) {
                siblings.add(newNode);
            }
        }
    }
    
    /**
     * crawl all children of given node and assign it as their parent
     * @param parentNode
     */
    public static void fixChildrenRelation(CompositeNode parentNode) {
        if (parentNode.getChildren() != null) {
            for (Node<?> child : parentNode.getChildren()) {
                if (child instanceof AbstractNodeTO<?>) {
                    ((AbstractNodeTO<?>) child).setParent(parentNode);
                }
            }
        }
    }


    /**
     * @param keys
     * @param dataMap
     * @return list of values of map, found by given keys 
     */
    public static <T, K> List<K> collectMapValues(List<T> keys,
            Map<T, K> dataMap) {
        List<K> valueSubList = new ArrayList<>();
        for (T key : keys) {
            valueSubList.add(dataMap.get(key));
        }
        
        return valueSubList;
    }
    
    /**
     * @param nodes
     * @return list of children in list of appropriate type
     */
    public static List<Node<?>> buildChildrenList(Node<?>...nodes) {
        return Lists.newArrayList(nodes);
    }

}
