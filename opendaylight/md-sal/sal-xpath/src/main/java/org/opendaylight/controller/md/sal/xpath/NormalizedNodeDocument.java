/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.w3c.dom.Node;

public class NormalizedNodeDocument extends ThrowExceptionDocument {

    private final NormalizedNode<?, ?> childNode;

    public NormalizedNodeDocument(NormalizedNode<?, ?> rootNode) {
        this.childNode = rootNode;
    }

    @Override
    public Node getFirstChild() {
        if (childNode != null) {
            return new NormalizedNodeElement(childNode, this);
        }
        return null;
    }

    @Override
    public boolean getXmlStandalone() {
        return true;
    }

    @Override
    public String getXmlVersion() {
        return "1.1";
    }

    @Override
    public String getXmlEncoding() {
        return null;
    }

    @Override
    public boolean hasChildNodes() {
        return childNode != null;
    }

    @Override
    public String getNodeName() {
        return "#document";
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getNamespaceURI() {
        return null;
    }

    @Override
    public short getNodeType() {
        // TODO Auto-generated method stub
        return Node.DOCUMENT_NODE;
    }

    @Override
    public Node getParentNode() {
        return null;
    }
}
