/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

public class NewElement implements Element {

    @Override
    public String getTagName() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getAttribute(String name) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setAttribute(String name, String value) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void removeAttribute(String name) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Attr getAttributeNode(String name) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public NodeList getElementsByTagName(String name) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName)
            throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setAttributeNS(String namespaceURI, String qualifiedName,
            String value) throws DOMException {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void removeAttributeNS(String namespaceURI, String localName)
            throws DOMException {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public Attr getAttributeNodeNS(String namespaceURI, String localName)
            throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
            throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean hasAttribute(String name) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String localName)
            throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void setIdAttributeNS(String namespaceURI, String localName,
            boolean isId) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setIdAttributeNode(Attr idAttr, boolean isId)
            throws DOMException {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public String getNodeName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNodeValue() throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException {
        // TODO Auto-generated method stub

    }

    @Override
    public short getNodeType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Node getParentNode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeList getChildNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node getFirstChild() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node getLastChild() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node getPreviousSibling() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node getNextSibling() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NamedNodeMap getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Document getOwnerDocument() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasChildNodes() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Node cloneNode(boolean deep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void normalize() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isSupported(String feature, String version) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getNamespaceURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getLocalName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasAttributes() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getBaseURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getTextContent() throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isSameNode(Node other) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String lookupPrefix(String namespaceURI) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String lookupNamespaceURI(String prefix) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEqualNode(Node arg) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getFeature(String feature, String version) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getUserData(String key) {
        // TODO Auto-generated method stub
        return null;
    }

}
