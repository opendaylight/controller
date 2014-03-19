/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import java.util.List;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.proc.common.EXIOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Lists;

/**
 * Start-exi netconf message.
 */
public final class NetconfStartExiMessage extends NetconfMessage {

    public static final String START_EXI = "start-exi";
    public static final String ALIGNMENT_KEY = "alignment";
    public static final String FIDELITY_KEY = "fidelity";
    public static final String COMMENTS_KEY = "comments";
    public static final String DTD_KEY = "dtd";
    public static final String LEXICAL_VALUES_KEY = "lexical-values";
    public static final String PIS_KEY = "pis";
    public static final String PREFIXES_KEY = "prefixes";

    private NetconfStartExiMessage(Document doc) {
        super(doc);
    }

    public static NetconfStartExiMessage create(EXIOptions exiOptions, String messageId) {
        Document doc = XmlUtil.newDocument();
        Element rpcElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                XmlNetconfConstants.RPC_KEY);
        rpcElement.setAttributeNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                XmlNetconfConstants.MESSAGE_ID, messageId);

        // TODO draft http://tools.ietf.org/html/draft-varga-netconf-exi-capability-02#section-3.5.1 has no namespace for start-exi element in xml
        Element startExiElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0,
                START_EXI);

        addAlignemnt(exiOptions, doc, startExiElement);
        addFidelity(exiOptions, doc, startExiElement);

        rpcElement.appendChild(startExiElement);

        doc.appendChild(rpcElement);
        return new NetconfStartExiMessage(doc);
    }

    private static void addFidelity(EXIOptions exiOptions, Document doc, Element startExiElement) {
        List<Element> fidelityElements = Lists.newArrayList();
        createFidelityElement(doc, fidelityElements, exiOptions.getPreserveComments(), COMMENTS_KEY);
        createFidelityElement(doc, fidelityElements, exiOptions.getPreserveDTD(), DTD_KEY);
        createFidelityElement(doc, fidelityElements, exiOptions.getPreserveLexicalValues(), LEXICAL_VALUES_KEY);
        createFidelityElement(doc, fidelityElements, exiOptions.getPreservePIs(), PIS_KEY);
        createFidelityElement(doc, fidelityElements, exiOptions.getPreserveNS(), PREFIXES_KEY);

        if (fidelityElements.isEmpty() == false) {
            Element fidelityElement = doc.createElementNS(
                    XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0, FIDELITY_KEY);
            for (Element element : fidelityElements) {
                fidelityElement.appendChild(element);
            }
            startExiElement.appendChild(fidelityElement);
        }
    }

    private static void addAlignemnt(EXIOptions exiOptions, Document doc, Element startExiElement) {
        Element alignmentElement = doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0,
                ALIGNMENT_KEY);
        alignmentElement.setTextContent(exiOptions.getAlignmentType().toString());
        startExiElement.appendChild(alignmentElement);
    }

    private static void createFidelityElement(Document doc, List<Element> fidelityElements, boolean fidelity, String fidelityName) {

        if (fidelity) {
            fidelityElements.add(doc.createElementNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0,
                    fidelityName));
        }

    }
}
