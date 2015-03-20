/*
 * Copyright (c) 2015 b<>com and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/*
 * This class adds the xmlns namespace tag to NETCONF <hello> messages
 * if it is missing.
 *
 * This is so that OpenDaylight accept <hello> messages coming from NETCONF
 * devices that do not set the namespace, such as Cisco routers.
 */

public class NetconfHelloFixer {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfHelloFixer.class);

    private static final DocumentBuilderFactory BUILDERFACTORY;

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        BUILDERFACTORY = factory;
    }

    public static boolean isHelloAndLacksNamespace(Document doc) {
        Element hello = doc.getDocumentElement();
        String tagName = hello.getTagName();
        if (! tagName.equals("hello"))
            return false;
        if (hello.hasAttribute("xmlns"))
            return false;
        return true;
    }

    public static Document recreateWithNamespace(Document doc, final String xmlns) throws ParserConfigurationException {
        NetconfHelloFixer fixer = new NetconfHelloFixer(doc, xmlns);
        return fixer.getFixedDocument();
    }

    private Document doc;
    private List<String> caps;
    private String sessionId;
    private final String xmlns;

    private NetconfHelloFixer(Document doc, final String xmlns) {
        this.doc = doc;
        caps = null;
        sessionId = null;
        this.xmlns = xmlns;
    }

    private Document getFixedDocument() throws ParserConfigurationException {
        readCapabilities();
        readSessionId();
        return rebuildHelloMessage();
    }

    private void readCapabilities() {
        Element hello = doc.getDocumentElement();
        caps = new ArrayList<String>();
        NodeList capNodes = hello.getElementsByTagName("capability");
        for (int i=0; i<capNodes.getLength(); i++) {
            Node capNode = capNodes.item(i);
            NodeList tentativeCapTextNodes = capNode.getChildNodes();
            for (int j=0; j<tentativeCapTextNodes.getLength(); j++){
                Node tentativeCapTextNode = tentativeCapTextNodes.item(j);
                if (tentativeCapTextNode instanceof Text) {
                    String cap = ((Text) tentativeCapTextNode).getTextContent();
                    LOG.debug("Found <capability>: {}", cap);
                    caps.add(cap);
                    break;
                }
            }
        }
    }

    private void readSessionId() {
        Element hello = doc.getDocumentElement();
        sessionId = hello.getElementsByTagName("session-id").item(0).getTextContent();
        LOG.debug("Found <session-id>: {}", sessionId);
    }

    /* Rebuild the hello message with all the elements in the NETCONF_NS namespace.
     * (else: xpath search for the session-id fails if it is not the case)
     */
    private Document rebuildHelloMessage() throws ParserConfigurationException {
        DocumentBuilder builder = BUILDERFACTORY.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        // Create the <hello> root
        Element hello = newDoc.createElementNS(xmlns, "hello");
        newDoc.appendChild(hello);

        Attr xmlns = newDoc.createAttribute("xmlns");
        xmlns.insertBefore(newDoc.createTextNode(this.xmlns), xmlns.getLastChild());
        hello.setAttributeNode(xmlns);

        // Create the <capabilities> and <session-id> on the new <hello> root
        Element capsElement = newDoc.createElementNS(this.xmlns, "capabilities");
        hello.appendChild(capsElement);
        for (String capString : caps) {
            Element cap = newDoc.createElementNS(this.xmlns, "capability");
            capsElement.appendChild(cap);
            cap.insertBefore(newDoc.createTextNode(capString), cap.getLastChild());
        }

        // Create the <session-id>
        Element sessionId = newDoc.createElementNS(this.xmlns, "session-id");
        hello.appendChild(sessionId);
        sessionId.insertBefore(newDoc.createTextNode(this.sessionId), sessionId.getLastChild());

        return newDoc;
    }
}
