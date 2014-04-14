/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class Validate extends AbstractConfigNetconfOperation {

    public static final String VALIDATE = "validate";

    private static final Logger logger = LoggerFactory.getLogger(Validate.class);

    private final TransactionProvider transactionProvider;

    public Validate(final TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.transactionProvider = transactionProvider;
    }

    private void checkXml(XmlElement xml) {
        xml.checkName(VALIDATE);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        XmlElement sourceElement = xml.getOnlyChildElement(XmlNetconfConstants.SOURCE_KEY,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        XmlElement sourceChildNode = sourceElement.getOnlyChildElement();

        sourceChildNode.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        String datastoreValue = sourceChildNode.getName();
        Datastore sourceDatastore = Datastore.valueOf(datastoreValue);

        Preconditions.checkState(sourceDatastore == Datastore.candidate, "Only " + Datastore.candidate
                + " is supported as source for " + VALIDATE + " but was " + datastoreValue);
    }

    @Override
    protected String getOperationName() {
        return VALIDATE;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {
        try {
            checkXml(xml);
        } catch (IllegalStateException e) {
            //FIXME where can IllegalStateException  be thrown? I see precondition that guards for programming bugs..
            logger.warn("Rpc error: {}", ErrorTag.missing_attribute, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.missing_attribute.name(), "Missing value of datastore attribute");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.missing_attribute,
                    ErrorSeverity.error, errorInfo);
        } catch (final IllegalArgumentException e) {
            // FIXME use checked exception if it has domain meaning
            logger.warn("Rpc error: {}", ErrorTag.bad_attribute, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.bad_attribute.name(), e.getMessage());
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.bad_attribute,
                    ErrorSeverity.error, errorInfo);
        }

        try {
            transactionProvider.validateTransaction();
        } catch (ValidationException e) {
            logger.warn("Validation failed", e);
            throw NetconfDocumentedException.wrap(e);
        } catch (IllegalStateException e) {
            logger.warn("Validation failed", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo
                    .put(ErrorTag.operation_failed.name(),
                            "Datastore is not present. Use 'get-config' or 'edit-config' before triggering 'operations' operation");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                    ErrorSeverity.error, errorInfo);

        }

        logger.trace("Datastore {} validated successfully", Datastore.candidate);

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }
}
