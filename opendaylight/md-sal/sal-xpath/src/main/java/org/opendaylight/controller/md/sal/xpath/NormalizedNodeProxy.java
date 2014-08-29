/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Defines a NormalizedNode implementations that represents a string or text field.
 * @author Devin Avery
 * @author Neel Bommisetty
 *
 */
public class NormalizedNodeProxy extends ThrowExceptionNode implements XPathNodeProxy {
    private final Node parent;
    private final NormalizedNode<?,?> nodeToProxy;

    public NormalizedNodeProxy(NormalizedNode<?,?> nodeValue, Node parentNode ) {
        this.nodeToProxy = nodeValue;
        parent = parentNode;
    }

    @Override
    public short getNodeType() {
        return Node.TEXT_NODE;
    }

    @Override
    public NormalizedNode<?, ?> getProxiedNode() {
        return nodeToProxy;
    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public String getNodeValue() {
        return nodeToProxy.getValue().toString();
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
        return this.getNodeName();
    }

    @Override
    public Node getNextSibling() {
        return null;
    }

    @Override
    public Node getFirstChild() {

        return null;
    }

//    @Override
//    public Node getLastChild() {
//
//        return null;
//    }

    @Override
    public String getTextContent() {
        return getNodeValue();
    }
}
