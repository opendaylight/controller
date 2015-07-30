/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.Datastore;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorSeverity;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorTag;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorType;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Validate extends AbstractConfigNetconfOperation {

    public static final String VALIDATE = "validate";

    private static final Logger LOG = LoggerFactory.getLogger(Validate.class);

    public Validate(final ConfigSubsystemFacade configSubsystemFacade, final String netconfSessionIdForReporting) {
        super(configSubsystemFacade, netconfSessionIdForReporting);
    }

    private void checkXml(XmlElement xml) throws DocumentedException {
        xml.checkName(VALIDATE);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        XmlElement sourceElement = xml.getOnlyChildElement(XmlNetconfConstants.SOURCE_KEY,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        XmlElement sourceChildNode = sourceElement.getOnlyChildElement();

        sourceChildNode.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        String datastoreValue = sourceChildNode.getName();
        Datastore sourceDatastore = Datastore.valueOf(datastoreValue);

        if (sourceDatastore != Datastore.candidate){
            throw new DocumentedException( "Only " + Datastore.candidate
                    + " is supported as source for " + VALIDATE + " but was " + datastoreValue, ErrorType.application, ErrorTag.data_missing, ErrorSeverity.error);
        }
    }

    @Override
    protected String getOperationName() {
        return VALIDATE;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        checkXml(xml);
        try {
            getConfigSubsystemFacade().validateConfiguration();
        } catch (ValidationException e) {
            LOG.warn("Validation failed", e);
            throw DocumentedException.wrap(e);
        } catch (IllegalStateException e) {
            LOG.warn("Validation failed", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo
                    .put(ErrorTag.operation_failed.name(),
                            "Datastore is not present. Use 'get-config' or 'edit-config' before triggering 'operations' operation");
            throw new DocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                    ErrorSeverity.error, errorInfo);

        }

        LOG.trace("Datastore {} validated successfully", Datastore.candidate);

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }
}
