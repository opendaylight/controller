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

public class DocumentProxy extends NodeProxy implements Document {

    private final Document doc;
    protected DocumentProxy( Document node) {
        super(node);
        this.doc = node;
    }

    @Override
    public DocumentType getDoctype() {
        printMethod();
        return doc.getDoctype();
    }

    @Override
    public DOMImplementation getImplementation() {
        printMethod();
        return doc.getImplementation();
    }

    @Override
    public Element getDocumentElement() {
        printMethod();
        return doc.getDocumentElement();
    }

    @Override
    public Element createElement(String tagName) throws DOMException {
        printMethod();
        return doc.createElement(tagName);
    }

    @Override
    public DocumentFragment createDocumentFragment() {
        printMethod();
        return doc.createDocumentFragment();
    }

    @Override
    public Text createTextNode(String data) {
        printMethod();
        return doc.createTextNode(data);
    }

    @Override
    public Comment createComment(String data) {
        printMethod();
        return doc.createComment(data);
    }

    @Override
    public CDATASection createCDATASection(String data) throws DOMException {
        printMethod();
        return doc.createCDATASection(data);
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data)
            throws DOMException {
        printMethod();
        return doc.createProcessingInstruction(target, data);
    }

    @Override
    public Attr createAttribute(String name) throws DOMException {
        printMethod();
        return doc.createAttribute(name);
    }

    @Override
    public EntityReference createEntityReference(String name) throws DOMException {
        printMethod();
        return doc.createEntityReference(name);
    }

    @Override
    public NodeList getElementsByTagName(String tagname) {
        printMethod();
        return doc.getElementsByTagName(tagname);
    }

    @Override
    public Node importNode(Node importedNode, boolean deep) throws DOMException {
        printMethod();
        return doc.importNode(importedNode, deep);
    }

    @Override
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
        printMethod();
        return doc.createElementNS(namespaceURI, qualifiedName);
    }

    @Override
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
        printMethod();
        return doc.createAttributeNS(namespaceURI, qualifiedName);
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        printMethod();
        return doc.getElementsByTagNameNS(namespaceURI, localName);
    }

    @Override
    public Element getElementById(String elementId) {
        printMethod();
        return doc.getElementById(elementId);
    }

    @Override
    public String getInputEncoding() {
        printMethod();
        return doc.getInputEncoding();
    }

    @Override
    public String getXmlEncoding() {
        printMethod();
        return doc.getXmlEncoding();
    }

    @Override
    public boolean getXmlStandalone() {
        printMethod();
        return doc.getXmlStandalone();
    }

    @Override
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
        printMethod();
        doc.setXmlStandalone(xmlStandalone);
    }

    @Override
    public String getXmlVersion() {
        printMethod();
        return doc.getXmlVersion();
    }

    @Override
    public void setXmlVersion(String xmlVersion) throws DOMException {
        printMethod();
        doc.setXmlVersion(xmlVersion);
    }

    @Override
    public boolean getStrictErrorChecking() {
        printMethod();
        return doc.getStrictErrorChecking();
    }

    @Override
    public void setStrictErrorChecking(boolean strictErrorChecking) {
        printMethod();
        doc.setStrictErrorChecking(strictErrorChecking);
    }

    @Override
    public String getDocumentURI() {
        printMethod();
        return doc.getDocumentURI();
    }

    @Override
    public void setDocumentURI(String documentURI) {
        printMethod();
        doc.setDocumentURI(documentURI);
    }

    @Override
    public Node adoptNode(Node source) throws DOMException {
        printMethod();
        return doc.adoptNode(source);
    }

    @Override
    public DOMConfiguration getDomConfig() {
        printMethod();
        return doc.getDomConfig();
    }

    @Override
    public void normalizeDocument() {
        printMethod();
        doc.normalizeDocument();
    }

    @Override
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
        printMethod();
        return doc.renameNode(n, namespaceURI, qualifiedName);
    }

}
