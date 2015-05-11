/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.test;

import org.custommonkey.xmlunit.ElementNameQualifier;
import org.custommonkey.xmlunit.ElementQualifier;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Custom xmlunit qualifier that doesn't care about order when deeper in the recursion
 */
public class NetconfXmlUnitRecursiveQualifier implements ElementQualifier {

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

            if (!NAME_QUALIFIER.qualifyForComparison((Element) currentControl,
                    (Element) currentTest)) {
                return false;
            }

            NodeList controlNodes;
            NodeList testNodes;

            if (currentControl.hasChildNodes() && currentTest.hasChildNodes()) {
                controlNodes = currentControl.getChildNodes();
                testNodes = currentTest.getChildNodes();
            } else {
                return !(currentControl.hasChildNodes() || currentTest.hasChildNodes());
            }

            if (countNodesWithoutConsecutiveTextNodes(controlNodes)
                    != countNodesWithoutConsecutiveTextNodes(testNodes)) {
                return false;
            }

            for (int i = 0; i < controlNodes.getLength(); i++) {
                boolean matchFound = false;
                for (int j = 0; j < testNodes.getLength(); j++) {
                    Node controlNode = controlNodes.item(i);
                    Node testNode = testNodes.item(j);

                    if (controlNode.getNodeType() != testNode.getNodeType()) {
                        continue;
                    }

                    if (controlNode.getNodeType() == Node.TEXT_NODE) {
                        if (concatenateText(controlNode).equals(concatenateText(testNode))) {
                            matchFound = true;
                            break;
                        }

                    } else if (compareNodes(controlNode, testNode)) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String concatenateText(Node textNode) {
        StringBuilder builder = new StringBuilder();
        Node next = textNode;

        do {
            if (next.getNodeValue() != null) {
                builder.append(next.getNodeValue().trim());
                next = next.getNextSibling();
            }
        } while (next != null && next.getNodeType() == Node.TEXT_NODE);

        return builder.toString();
    }

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
