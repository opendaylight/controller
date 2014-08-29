/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

public class ThrowExceptionDocument extends ThrowExceptionNode implements Document {

    @Override
    public DocumentType getDoctype() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public DOMImplementation getImplementation() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Element getDocumentElement() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Element createElement(String tagName) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public DocumentFragment createDocumentFragment() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Text createTextNode(String data) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Comment createComment(String data) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public CDATASection createCDATASection(String data) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data)
            throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Attr createAttribute(String name) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public EntityReference createEntityReference(String name) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public NodeList getElementsByTagName(String tagname) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Node importNode(Node importedNode, boolean deep) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Element getElementById(String elementId) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public String getInputEncoding() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public String getXmlEncoding() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public boolean getXmlStandalone() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public String getXmlVersion() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public void setXmlVersion(String xmlVersion) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public boolean getStrictErrorChecking() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public void setStrictErrorChecking(boolean strictErrorChecking) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public String getDocumentURI() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public void setDocumentURI(String documentURI) {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Node adoptNode(Node source) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public DOMConfiguration getDomConfig() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public void normalizeDocument() {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

    @Override
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
        throw new SalXPathRuntimeException(getNotImplementedMessage());
    }

}
