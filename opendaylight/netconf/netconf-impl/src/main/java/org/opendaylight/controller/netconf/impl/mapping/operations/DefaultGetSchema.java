/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class DefaultGetSchema extends AbstractLastNetconfOperation {
    public static final String GET_SCHEMA = "get-schema";
    public static final String IDENTIFIER = "identifier";
    public static final String VERSION = "version";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultGetSchema.class);
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

        entry = new GetSchemaEntry(xml);

        String schema;
        try {
            schema = cap.getSchemaForCapability(entry.identifier, entry.version);
        } catch (IllegalStateException e) {
            Map<String, String> errorInfo = Maps.newHashMap();
            errorInfo.put(entry.identifier, e.getMessage());
            LOG.warn("Rpc error: {}", NetconfDocumentedException.ErrorTag.operation_failed, e);
            throw new NetconfDocumentedException(e.getMessage(), NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error, errorInfo);
        }

        Element getSchemaResult;
        getSchemaResult = XmlUtil.createTextElement(document, XmlNetconfConstants.DATA_KEY, schema,
                Optional.of(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING));
        LOG.trace("{} operation successful", GET_SCHEMA);

        return getSchemaResult;
    }

    private static final class GetSchemaEntry {
        private final String identifier;
        private final Optional<String> version;

        GetSchemaEntry(XmlElement getSchemaElement) throws NetconfDocumentedException {
            getSchemaElement.checkName(GET_SCHEMA);
            getSchemaElement.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING);

            XmlElement identifierElement = null;
            try {
                identifierElement = getSchemaElement.getOnlyChildElementWithSameNamespace(IDENTIFIER);
            } catch (MissingNameSpaceException e) {
                LOG.trace("Can't get identifier element as only child element with same namespace due to ",e);
                throw NetconfDocumentedException.wrap(e);
            }
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
