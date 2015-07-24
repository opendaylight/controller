/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.facade.xml.ConfigExecution;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorSeverity;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorTag;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorType;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EditConfig extends AbstractConfigNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(EditConfig.class);

    private EditConfigXmlParser editConfigXmlParser;

    public EditConfig(final ConfigSubsystemFacade configSubsystemFacade, final String netconfSessionIdForReporting) {
        super(configSubsystemFacade, netconfSessionIdForReporting);
        this.editConfigXmlParser = new EditConfigXmlParser();
    }

    @VisibleForTesting
    Element getResponseInternal(final Document document,
            final ConfigExecution configExecution) throws DocumentedException {

        try {
            getConfigSubsystemFacade().executeConfigExecution(configExecution);
        } catch (ValidationException e) {
            LOG.warn("Test phase for {} failed", EditConfigXmlParser.EDIT_CONFIG, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(), e.getMessage());
            throw new DocumentedException("Test phase: " + e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
        }

        LOG.trace("Operation {} successful", EditConfigXmlParser.EDIT_CONFIG);

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    @Override
    protected String getOperationName() {
        return EditConfigXmlParser.EDIT_CONFIG;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        ConfigExecution configExecution;
        // FIXME config mapping getter works on dynamic yang store service and so does later executeConfigExecution method
        // They might have different view of current yangs in ODL and might cause race conditions
        Config cfg = getConfigSubsystemFacade().getConfigMapping();
        configExecution = editConfigXmlParser.fromXml(xml, cfg);

        return getResponseInternal(document, configExecution);
    }

}
