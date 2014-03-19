/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.mapping;

import java.util.Map;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Optional;

public abstract class AbstractNetconfOperation implements NetconfOperation {
    private final String netconfSessionIdForReporting;

    protected AbstractNetconfOperation(String netconfSessionIdForReporting) {
        this.netconfSessionIdForReporting = netconfSessionIdForReporting;
    }

    public final String getNetconfSessionIdForReporting() {
        return netconfSessionIdForReporting;
    }

    @Override
    public HandlingPriority canHandle(Document message) {
        OperationNameAndNamespace operationNameAndNamespace = new OperationNameAndNamespace(message);
        return canHandle(operationNameAndNamespace.getOperationName(), operationNameAndNamespace.getNamespace());
    }

    public static final class OperationNameAndNamespace {
        private final String operationName, namespace;

        public OperationNameAndNamespace(Document message) {
            XmlElement requestElement = getRequestElementWithCheck(message);

            XmlElement operationElement = requestElement.getOnlyChildElement();
            operationName = operationElement.getName();
            namespace = operationElement.getNamespace();
        }

        public String getOperationName() {
            return operationName;
        }

        public String getNamespace() {
            return namespace;
        }
    }

    protected static XmlElement getRequestElementWithCheck(Document message) {
        return XmlElement.fromDomElementWithExpected(message.getDocumentElement(), XmlNetconfConstants.RPC_KEY,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    protected HandlingPriority canHandle(String operationName, String operationNamespace) {
        return operationName.equals(getOperationName()) && operationNamespace.equals(getOperationNamespace())
                ? getHandlingPriority()
                : HandlingPriority.CANNOT_HANDLE;
    }

    protected HandlingPriority getHandlingPriority() {
        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
    }

    protected String getOperationNamespace() {
        return XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0;
    }

    protected abstract String getOperationName();

    @Override
    public Document handle(Document requestMessage,
            NetconfOperationChainedExecution subsequentOperation) throws NetconfDocumentedException {

        XmlElement requestElement = getRequestElementWithCheck(requestMessage);

        Document document = XmlUtil.newDocument();

        XmlElement operationElement = requestElement.getOnlyChildElement();
        Map<String, Attr> attributes = requestElement.getAttributes();

        Element response = handle(document, operationElement, subsequentOperation);
        Element rpcReply = XmlUtil.createElement(document, XmlNetconfConstants.RPC_REPLY_KEY, Optional.of(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0));

        if(XmlElement.fromDomElement(response).hasNamespace()) {
            rpcReply.appendChild(response);
        } else {
            Element responseNS = XmlUtil.createElement(document, response.getNodeName(), Optional.of(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0));
            NodeList list = response.getChildNodes();
            while(list.getLength()!=0) {
                responseNS.appendChild(list.item(0));
            }
            rpcReply.appendChild(responseNS);
        }

        for (String attrName : attributes.keySet()) {
            rpcReply.setAttributeNode((Attr) document.importNode(attributes.get(attrName), true));
        }
        document.appendChild(rpcReply);
        return document;
    }

    protected abstract Element handle(Document document, XmlElement message, NetconfOperationChainedExecution subsequentOperation)
            throws NetconfDocumentedException;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(getClass().getName());
        try {
            sb.append("{name=").append(getOperationName());
        } catch(UnsupportedOperationException e) {
            // no problem
        }
        sb.append(", namespace=").append(getOperationNamespace());
        sb.append(", session=").append(netconfSessionIdForReporting);
        sb.append('}');
        return sb.toString();
    }
}
