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
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

public class ThrowExceptionElement extends NormalizedNodeProxy implements Element {

    public ThrowExceptionElement(NormalizedNode<?, ?> nodeValue, Node parentNode) {
        super(nodeValue, parentNode);
    }

    @Override
    public String getTagName() {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public String getAttribute(String name) {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public void setAttribute(String name, String value) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

    @Override
    public void removeAttribute(String name) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

    @Override
    public Attr getAttributeNode(String name) {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public NodeList getElementsByTagName(String name) {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value)
            throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

    @Override
    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

    @Override
    public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
            throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public boolean hasAttribute(String name) {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        throw new SalXPathRuntimeException( "Not Implemented" );
    }

    @Override
    public void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

    @Override
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId)
            throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

    @Override
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
        throw new SalXPathRuntimeException( "Not Implemented" );

    }

}
