/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * NetconfMessage that can carry additional header with session metadata. See {@link org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader}
 */
public final class NetconfHelloMessage extends NetconfMessage {

    public static final String HELLO_TAG = "hello";

    private final NetconfHelloMessageAdditionalHeader additionalHeader;

    public NetconfHelloMessage(Document doc, NetconfHelloMessageAdditionalHeader additionalHeader) throws NetconfDocumentedException {
        super(doc);
        checkHelloMessage(doc);
        this.additionalHeader = additionalHeader;
    }

    public NetconfHelloMessage(Document doc) throws NetconfDocumentedException {
        this(doc, null);
    }

    public Optional<NetconfHelloMessageAdditionalHeader> getAdditionalHeader() {
        return additionalHeader== null ? Optional.<NetconfHelloMessageAdditionalHeader>absent() : Optional.of(additionalHeader);
    }

    private static void checkHelloMessage(Document doc) {
        Preconditions.checkArgument(isHelloMessage(doc),
                "Hello message invalid format, should contain %s tag from namespace %s, but is: %s", HELLO_TAG,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, XmlUtil.toString(doc));
    }

    public static NetconfHelloMessage createClientHello(Iterable<String> capabilities,
                                                        Optional<NetconfHelloMessageAdditionalHeader> additionalHeaderOptional) throws NetconfDocumentedException {
        Document doc = createHelloMessageDoc(capabilities);
        return additionalHeaderOptional.isPresent() ? new NetconfHelloMessage(doc, additionalHeaderOptional.get())
                : new NetconfHelloMessage(doc);
    }

    private static Document createHelloMessageDoc(Iterable<String> capabilities) {
        Document doc = XmlUtil.newDocument();
        Element helloElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                HELLO_TAG);
        Element capabilitiesElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                XmlNetconfConstants.CAPABILITIES);

        for (String capability : Sets.newHashSet(capabilities)) {
            Element capElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                    XmlNetconfConstants.CAPABILITY);
            capElement.setTextContent(capability);
            capabilitiesElement.appendChild(capElement);
        }

        helloElement.appendChild(capabilitiesElement);

        doc.appendChild(helloElement);
        return doc;
    }

    public static NetconfHelloMessage createServerHello(Set<String> capabilities, long sessionId) throws NetconfDocumentedException {
        Document doc = createHelloMessageDoc(capabilities);
        Element sessionIdElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                XmlNetconfConstants.SESSION_ID);
        sessionIdElement.setTextContent(Long.toString(sessionId));
        doc.getDocumentElement().appendChild(sessionIdElement);
        return new NetconfHelloMessage(doc);
    }

    public static boolean isHelloMessage(final NetconfMessage msg) {
        Document document = msg.getDocument();
        return isHelloMessage(document);
    }

    private static boolean isHelloMessage(final Document document) {
        XmlElement element = XmlElement.fromDomElement(document.getDocumentElement());
        try {
            return element.getName().equals(HELLO_TAG) &&
                   element.hasNamespace() &&
                   element.getNamespace().equals(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        } catch (MissingNameSpaceException e) {
            // Cannot happen, since we check for hasNamespace
            throw new IllegalStateException(e);
        }
    }
}
