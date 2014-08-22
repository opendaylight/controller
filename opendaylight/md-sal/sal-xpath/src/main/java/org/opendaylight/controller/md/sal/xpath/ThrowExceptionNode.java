/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

public class ThrowExceptionNode implements Node {
    @Override
    public String getNodeName() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getNodeValue() throws DOMException {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public short getNodeType() {
        throw new RuntimeException("Not Implemented");
        // return 0;
    }

    @Override
    public Node getParentNode() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public NodeList getChildNodes() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node getFirstChild() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node getLastChild() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node getPreviousSibling() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node getNextSibling() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public NamedNodeMap getAttributes() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Document getOwnerDocument() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean hasChildNodes() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Node cloneNode(boolean deep) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void normalize() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isSupported(String feature, String version) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getNamespaceURI() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getPrefix() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getLocalName() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean hasAttributes() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getBaseURI() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getTextContent() throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isSameNode(Node other) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String lookupPrefix(String namespaceURI) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String lookupNamespaceURI(String prefix) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isEqualNode(Node arg) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Object getFeature(String feature, String version) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Object getUserData(String key) {
        throw new RuntimeException("Not Implemented");
    }
}
