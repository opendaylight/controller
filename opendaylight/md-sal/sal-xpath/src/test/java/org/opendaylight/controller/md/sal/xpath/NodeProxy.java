/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.md.sal.xpath;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

public class NodeProxy implements Node{

    private static final Map<Short, String> TYPE_MAP = new HashMap<>();
    static{
        TYPE_MAP.put( Node.ELEMENT_NODE, "ElementNode" );
        TYPE_MAP.put( Node.DOCUMENT_NODE, "DocumentNode" );
        TYPE_MAP.put( Node.TEXT_NODE, "TextNode" );
    }
    protected final Node node;

    protected NodeProxy( Node node ){
        this.node = node;
    }

    protected void printMethod( Object ... values ){
        String type = TYPE_MAP.get( node.getNodeType() );
        if( type == null ){
            type = "unknown type";
        }

        StringBuilder nodeName = new StringBuilder();
        if( node.getNodeName() != null ){
            nodeName.append( node.getNodeName() );
        } else if ( node.getLocalName() != null ){
            if( nodeName.length() > 0 ){
                nodeName.append( "-" );
            }
            nodeName.append( node.getLocalName() );
        }

        System.out.println( nodeName + "." + Thread.currentThread().getStackTrace()[2].getMethodName() + "-" +
                            /*node.getClass().getSimpleName() + "-" +*/ type + "-" +
                            Arrays.toString( values ));
    }

    @Override
    public String getNodeName() {
        String nodeName = node.getNodeName();
        printMethod( nodeName );
        return nodeName;
    }

    @Override
    public String getNodeValue() throws DOMException {
        printMethod();
        return node.getNodeValue();
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException {
        printMethod();
        node.setNodeValue( nodeValue );
    }

    @Override
    public short getNodeType() {
        short nodeType = node.getNodeType();
        printMethod( nodeType );
        return nodeType;
    }

    @Override
    public Node getParentNode() {
        printMethod();
        Node parent = node.getParentNode();
        return wrapNode(parent);
    }

    public static NodeProxy wrapNode(Node node) {
        if( node instanceof NodeProxy ){
            return (NodeProxy)node;
        } else if( node instanceof Element ){
            return new ElementProxy( (Element) node );
        } else if( node != null ){
            return new NodeProxy( node );
        } else {
            return null;
        }
    }

    @Override
    public NodeList getChildNodes() {
        printMethod();
        return node.getChildNodes();
    }

    @Override
    public Node getFirstChild() {
        printMethod();
        Node child = node.getFirstChild();
        return wrapNode(child);
    }

    @Override
    public Node getLastChild() {
        printMethod();
        Node child = node.getLastChild();
        return wrapNode(child);
    }

    @Override
    public Node getPreviousSibling() {
        printMethod();
        Node relatedNode = node.getPreviousSibling();
        return wrapNode(relatedNode);
    }

    @Override
    public Node getNextSibling() {
        printMethod();
        Node relatedNode = node.getNextSibling();
        return wrapNode(relatedNode);
    }

    @Override
    public NamedNodeMap getAttributes() {
        NamedNodeMap attributes = node.getAttributes();
        printMethod( attributes, attributes.getLength() );
        return attributes;
    }

    @Override
    public Document getOwnerDocument() {
        printMethod();
        return node.getOwnerDocument();
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        printMethod();
        return node.insertBefore( newChild, refChild );
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        printMethod();
        return node.replaceChild( newChild, oldChild );
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        printMethod();
        return node.removeChild( oldChild );
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        printMethod();
        return node.appendChild( newChild );
    }

    @Override
    public boolean hasChildNodes() {
        printMethod();
        return node.hasChildNodes();
    }

    @Override
    public Node cloneNode(boolean deep) {
        printMethod();
        return node.cloneNode( deep );
    }

    @Override
    public void normalize() {
        printMethod();
        node.normalize();
    }

    @Override
    public boolean isSupported(String feature, String version) {
        printMethod();
        return node.isSupported( feature, version );
    }

    @Override
    public String getNamespaceURI() {
        String namespaceURI = node.getNamespaceURI();
        printMethod( namespaceURI );
        return namespaceURI;
    }

    @Override
    public String getPrefix() {
        printMethod();
        return node.getPrefix();
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        printMethod();
        node.setPrefix( prefix );
    }

    @Override
    public String getLocalName() {
        String localName = node.getLocalName();
        printMethod( localName );
        return localName;
    }

    @Override
    public boolean hasAttributes() {
        printMethod();
        return node.hasAttributes();
    }

    @Override
    public String getBaseURI() {
        printMethod();
        return node.getBaseURI();
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        printMethod();
        return node.compareDocumentPosition( other );
    }

    @Override
    public String getTextContent() throws DOMException {
        printMethod();
        return node.getTextContent();
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
        printMethod();
        node.setTextContent( textContent );
    }

    @Override
    public boolean isSameNode(Node other) {
        printMethod();
        return node.isSameNode(other);
    }

    @Override
    public String lookupPrefix(String namespaceURI) {
        printMethod();
        return node.lookupPrefix(namespaceURI);
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI) {
        printMethod();
        return node.isDefaultNamespace(namespaceURI);
    }

    @Override
    public String lookupNamespaceURI(String prefix) {
        printMethod();
        return node.lookupNamespaceURI(prefix);
    }

    @Override
    public boolean isEqualNode(Node arg) {
        printMethod();
        return node.isEqualNode(arg);
    }

    @Override
    public Object getFeature(String feature, String version) {
        printMethod();
        return node.getFeature(feature, version);
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        printMethod();
        return node.setUserData(key, data, handler);
    }

    @Override
    public Object getUserData(String key) {
        printMethod();
        return node.getUserData(key);
    }

}
