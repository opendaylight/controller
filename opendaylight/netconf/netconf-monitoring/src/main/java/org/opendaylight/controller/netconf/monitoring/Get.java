/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring;

import java.util.Map;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.monitoring.xml.JaxBSerializer;
import org.opendaylight.controller.netconf.monitoring.xml.model.NetconfState;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class Get extends AbstractNetconfOperation {

    private static final Logger logger = LoggerFactory.getLogger(Get.class);
    private final NetconfMonitoringService netconfMonitor;

    public Get(NetconfMonitoringService netconfMonitor) {
        super(MonitoringConstants.MODULE_NAME);
        this.netconfMonitor = netconfMonitor;
    }

    private Element getPlaceholder(Document innerResult) {
        try {
            XmlElement rootElement = XmlElement.fromDomElementWithExpected(innerResult.getDocumentElement(),
                    XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
            return rootElement.getOnlyChildElement(XmlNetconfConstants.DATA_KEY).getDomElement();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format(
                    "Input xml in wrong format, Expecting root element %s with child element %s, but was %s",
                    XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.DATA_KEY,
                    XmlUtil.toString(innerResult.getDocumentElement())), e);
        }
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.GET;
    }

    @Override
    protected HandlingPriority getHandlingPriority() {
        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.increasePriority(1);
    }

    @Override
    public Document handle(Document requestMessage, NetconfOperationChainedExecution subsequentOperation)
            throws NetconfDocumentedException {
        Preconditions.checkArgument(subsequentOperation.isExecutionTermination() == false,
                "Subsequent netconf operation expected by %s", this);

        try {
            Document innerResult = subsequentOperation.execute(requestMessage);

            NetconfState netconfMonitoring = new NetconfState(netconfMonitor);
            Element monitoringXmlElement = new JaxBSerializer().toXml(netconfMonitoring);

            monitoringXmlElement = (Element) innerResult.importNode(monitoringXmlElement, true);
            Element monitoringXmlElementPlaceholder = getPlaceholder(innerResult);
            monitoringXmlElementPlaceholder.appendChild(monitoringXmlElement);

            return innerResult;
        } catch (RuntimeException e) {
            String errorMessage = "Get operation for netconf-state subtree failed";
            logger.warn(errorMessage, e);
            Map<String, String> info = Maps.newHashMap();
            info.put(NetconfDocumentedException.ErrorSeverity.error.toString(), e.getMessage());
            throw new NetconfDocumentedException(errorMessage, NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error, info);
        }
    }

    @Override
    protected Element handle(Document document, XmlElement message, NetconfOperationChainedExecution subsequentOperation)
            throws NetconfDocumentedException {
        throw new UnsupportedOperationException("Never gets called");
    }
}
