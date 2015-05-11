/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.test;

import org.custommonkey.xmlunit.ElementNameQualifier;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Based on XmlUnit's RecursiveElementNameAndTextQualifier with fixes for depth
 */
public class NetconfXmlUnitRecursiveQualifier extends RecursiveElementNameAndTextQualifier{

    private static final ElementNameQualifier NAME_QUALIFIER =
            new ElementNameQualifier();

    public NetconfXmlUnitRecursiveQualifier() {
    }

    @Override
    public boolean qualifyForComparison(Element currentControl,
                                        Element currentTest) {
        return compareNodes(currentControl, currentTest);
    }

    private boolean compareNodes(Node currentControl, Node currentTest) {
        try {

            // if they are elements, compare names of the two nodes
            if (!NAME_QUALIFIER.qualifyForComparison((Element) currentControl,
                    (Element) currentTest)) {
                return false;
            }

            // Compare the control and test elements' children

            NodeList controlNodes = null;
            NodeList testNodes = null;

            // Check that both nodes have children and, if so, get lists of them

            if (currentControl.hasChildNodes() && currentTest.hasChildNodes()) {
                controlNodes = currentControl.getChildNodes();
                testNodes = currentTest.getChildNodes();
            } else if (currentControl.hasChildNodes()
                    || currentTest.hasChildNodes()) {
                return false;

                // if both nodes are empty, they are comparable
            } else {
                return true;
            }

            // check that both node lists have the same length

            if (countNodesWithoutConsecutiveTextNodes(controlNodes)
                    != countNodesWithoutConsecutiveTextNodes(testNodes)) {
                return false;
            }

            // Do checks of test and control nodes' children

            final int cNodes = controlNodes.getLength();
            final int tNodes = testNodes.getLength();

            for (int i = 0; i < cNodes; i++) {
                boolean matchFound = false;
                for (int j = 0; j < tNodes; j++) {
                    Node controlNode = controlNodes.item(i);
                    Node testNode = testNodes.item(j);


                    // check if both node are same type
                    if (controlNode.getNodeType() != testNode.getNodeType()) {
                        continue;
                    }

                    // compare text nodes
                    if (controlNode.getNodeType() == Node.TEXT_NODE) {
                        // compare concatenated, trimmed text nodes
                        if (catText(controlNode).equals(catText(testNode))) {
                            matchFound = true;
                            break;
                        }

                        // recursive check of current child control and test nodes'
                        // children

                    } else if (compareNodes((Element) controlNode,
                            (Element) testNode)) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    return false;
                }
            }

            // All descendants of current control and test nodes are comparable
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Concatenates contiguous Text nodes and removes all leading and
     * trailing whitespace.
     * @param textNode
     * @return
     */
    private static String catText(Node textNode) {
        StringBuffer text = new StringBuffer();
        Node next = textNode;

        do {
            if (next.getNodeValue() != null) {
                text.append(next.getNodeValue().trim());
                next = next.getNextSibling();
            }
        } while (next != null && next.getNodeType() == Node.TEXT_NODE);

        return text.toString();
    }

    /**
     * Calculates the number of Nodes that are either not Text nodes
     * or are Text nodes whose previous sibling isn't a Text node as
     * well.  I.e. consecutive Text nodes are counted as a single
     * node.
     */
    private static int countNodesWithoutConsecutiveTextNodes(NodeList l) {
        int count = 0;
        boolean lastNodeWasText = false;
        final int length = l.getLength();
        for (int i = 0; i < length; i++) {
            Node n = l.item(i);
            if (!lastNodeWasText || n.getNodeType() != Node.TEXT_NODE) {
                count++;
            }
            lastNodeWasText = n.getNodeType() == Node.TEXT_NODE;
        }
        return count;
    }
}
