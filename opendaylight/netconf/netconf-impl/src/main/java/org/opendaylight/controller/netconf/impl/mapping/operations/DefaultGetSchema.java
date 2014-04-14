/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public final class DefaultGetSchema extends AbstractLastNetconfOperation {
    public static final String GET_SCHEMA = "get-schema";
    public static final String IDENTIFIER = "identifier";
    public static final String VERSION = "version";

    private static final Logger logger = LoggerFactory.getLogger(DefaultGetSchema.class);
    private final CapabilityProvider cap;

    public DefaultGetSchema(CapabilityProvider cap, String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
        this.cap = cap;
    }

    @Override
    protected String getOperationName() {
        return GET_SCHEMA;
    }

    @Override
    protected String getOperationNamespace() {
        return XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {
        GetSchemaEntry entry;

        try {
            entry = new GetSchemaEntry(xml);
        } catch (final IllegalArgumentException e) {
            logger.warn("Error parsing xml", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(NetconfDocumentedException.ErrorTag.bad_attribute.name(), e.getMessage());
            throw new NetconfDocumentedException(e.getMessage(), e, NetconfDocumentedException.ErrorType.rpc,
                    NetconfDocumentedException.ErrorTag.bad_attribute, NetconfDocumentedException.ErrorSeverity.error,
                    errorInfo);
        } catch (final IllegalStateException e) {
            logger.warn("Error parsing xml", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(NetconfDocumentedException.ErrorTag.bad_attribute.name(), e.getMessage());
            throw new NetconfDocumentedException(e.getMessage(), e, NetconfDocumentedException.ErrorType.rpc,
                    NetconfDocumentedException.ErrorTag.bad_attribute, NetconfDocumentedException.ErrorSeverity.error,
                    errorInfo);
        }

        String schema;
        try {
            schema = cap.getSchemaForCapability(entry.identifier, entry.version);
        } catch (IllegalStateException e) {
            Map<String, String> errorInfo = Maps.newHashMap();
            errorInfo.put(entry.identifier, e.getMessage());
            logger.warn("Rpc error: {}", NetconfDocumentedException.ErrorTag.operation_failed, e);
            throw new NetconfDocumentedException(e.getMessage(), NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error, errorInfo);
        }

        Element getSchemaResult;
        getSchemaResult = XmlUtil.createTextElement(document, XmlNetconfConstants.DATA_KEY, schema,
                Optional.of(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING));
        logger.trace("{} operation successful", GET_SCHEMA);

        return getSchemaResult;
    }

    private static final class GetSchemaEntry {
        private final String identifier;
        private final Optional<String> version;

        GetSchemaEntry(XmlElement getSchemaElement) {
            getSchemaElement.checkName(GET_SCHEMA);
            getSchemaElement.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING);

            XmlElement identifierElement = getSchemaElement.getOnlyChildElementWithSameNamespace(IDENTIFIER);
            identifier = identifierElement.getTextContent();
            Optional<XmlElement> versionElement = getSchemaElement
                    .getOnlyChildElementWithSameNamespaceOptionally(VERSION);
            if (versionElement.isPresent()) {
                version = Optional.of(versionElement.get().getTextContent());
            } else {
                version = Optional.absent();
            }
        }
    }
}
