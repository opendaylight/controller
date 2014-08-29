/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class NormalizedNodeText extends ThrowExceptionText {
    Node parent;
    Node nextSibling;
    String nodeValue;

    public NormalizedNodeText(String Nodevalue, Element parentNode,
            Node nextsibling) {

        nodeValue = Nodevalue;
        parent = parentNode;
        nextSibling = nextsibling;
    }

    @Override
    public short getNodeType() {
        return Node.TEXT_NODE;

    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public String getNodeValue() {
        return nodeValue;
    }

    @Override
    public String getNamespaceURI() {
        return null;

    }

    @Override
    public boolean hasChildNodes() {
        return false;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return null;
    }

    @Override
    public String getNodeName() {

        return "#text";
    }

    @Override
    public Node getParentNode() {
        return parent;
    }

    @Override
    public String getLocalName() {
        return "#text";
    }

    @Override
    public Node getNextSibling() {
        return nextSibling;
    }

    @Override
    public Node getFirstChild() {

        return null;
    }

    @Override
    public Node getLastChild() {

        return null;
    }

    @Override
    public String getTextContent() {
        return nodeValue;

    }
}
