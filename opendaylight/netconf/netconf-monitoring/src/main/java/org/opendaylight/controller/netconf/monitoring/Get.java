/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring;

import com.google.common.collect.Maps;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilterChain;
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

import java.util.Map;

public class Get implements NetconfOperationFilter {

    private static final Logger logger = LoggerFactory.getLogger(Get.class);
    private final NetconfMonitoringService netconfMonitor;

    public Get(NetconfMonitoringService netconfMonitor) {
        this.netconfMonitor = netconfMonitor;
    }

    @Override
    public Document doFilter(Document message, NetconfOperationRouter operationRouter,
            NetconfOperationFilterChain filterChain) throws NetconfDocumentedException {
        AbstractNetconfOperation.OperationNameAndNamespace operationNameAndNamespace = new AbstractNetconfOperation.OperationNameAndNamespace(
                message);
        if (canHandle(operationNameAndNamespace)) {
            return handle(message, operationRouter, filterChain);
        }
        return filterChain.execute(message, operationRouter);
    }

    private Document handle(Document message, NetconfOperationRouter operationRouter,
            NetconfOperationFilterChain filterChain) throws NetconfDocumentedException {
        try {
            Document innerResult = filterChain.execute(message, operationRouter);

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

    private Element getPlaceholder(Document innerResult) {
        try {
            XmlElement rootElement = XmlElement.fromDomElementWithExpected(innerResult.getDocumentElement(),
                    XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.RFC4741_TARGET_NAMESPACE);
            return rootElement.getOnlyChildElement(XmlNetconfConstants.DATA_KEY).getDomElement();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format(
                    "Input xml in wrong format, Expecting root element %s with child element %s, but was %s",
                    XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.DATA_KEY,
                    XmlUtil.toString(innerResult.getDocumentElement())), e);
        }
    }

    private boolean canHandle(AbstractNetconfOperation.OperationNameAndNamespace operationNameAndNamespace) {
        if (operationNameAndNamespace.getOperationName().equals(XmlNetconfConstants.GET) == false)
            return false;
        return operationNameAndNamespace.getNamespace().equals(
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    @Override
    public int getSortingOrder() {
        // FIXME filters for different operations cannot have same order
        return 1;
    }

    @Override
    public int compareTo(NetconfOperationFilter o) {
        return Integer.compare(getSortingOrder(), o.getSortingOrder());
    }

}
