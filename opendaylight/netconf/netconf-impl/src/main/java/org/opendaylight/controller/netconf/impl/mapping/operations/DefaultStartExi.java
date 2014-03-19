/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.mapping.operations;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.mapping.api.DefaultNetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.util.handler.NetconfEXICodec;
import org.opendaylight.controller.netconf.util.handler.NetconfEXIToMessageDecoder;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToEXIEncoder;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.EXIParameters;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.proc.common.EXIOptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class DefaultStartExi extends AbstractNetconfOperation implements DefaultNetconfOperation {
    public static final String START_EXI = "start-exi";

    private static final Logger logger = LoggerFactory.getLogger(DefaultStartExi.class);
    private NetconfSession netconfSession;

    public DefaultStartExi(String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected HandlingPriority canHandle(String operationName,
            String netconfOperationNamespace) {
        if (operationName.equals(START_EXI) == false)
            return HandlingPriority.CANNOT_HANDLE;
        if (netconfOperationNamespace
                .equals(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0) == false)
            return HandlingPriority.CANNOT_HANDLE;

        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
    }

    @Override
    protected Element handle(Document document, XmlElement operationElement,
            NetconfOperationRouter opRouter) throws NetconfDocumentedException {
        Element getSchemaResult = document.createElement(XmlNetconfConstants.OK);
        XmlUtil.addNamespaceAttr(getSchemaResult,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        logger.debug("received start-exi message {} ", XmlUtil.toString(document));

        final EXIParameters exiParams;
        try {
            exiParams = EXIParameters.forXmlElement(operationElement);
        } catch (EXIOptionsException e) {
            logger.debug("Failed to parse EXI parameters", e);
            throw new NetconfDocumentedException("Failed to parse EXI parameters", ErrorType.protocol,
                    ErrorTag.operation_failed, ErrorSeverity.error);
        }
        //TODO resolve AlignmentType
        NetconfEXICodec exiCodec = new NetconfEXICodec(exiParams.getOptions());
        netconfSession.addExiDecoder(NetconfEXIToMessageDecoder.HANDLER_NAME,new NetconfEXIToMessageDecoder(exiCodec));
        netconfSession.addExiEncoderAfterMessageSent(NetconfMessageToEXIEncoder.HANDLER_NAME, new NetconfMessageToEXIEncoder(exiCodec));

        logger.trace("{} operation successful", START_EXI);
        return getSchemaResult;

    }

    @Override
    public void setNetconfSession(NetconfSession s) {
        netconfSession = s;
    }

    public NetconfSession getNetconfSession() {
        return netconfSession;
    }
}
