/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.facade.xml.ConfigExecution;
import org.opendaylight.controller.config.facade.xml.Datastore;
import org.opendaylight.controller.config.facade.xml.TestOption;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditConfigXmlParser {

    private static final Logger LOG = LoggerFactory.getLogger(EditConfigXmlParser.class);

    public static final String EDIT_CONFIG = "edit-config";
    public static final String DEFAULT_OPERATION_KEY = "default-operation";
    static final String ERROR_OPTION_KEY = "error-option";
    static final String DEFAULT_ERROR_OPTION = "stop-on-error";
    static final String TARGET_KEY = "target";
    static final String TEST_OPTION_KEY = "test-option";

    public EditConfigXmlParser() {
    }

    ConfigExecution fromXml(final XmlElement xml, final Config cfgMapping)
            throws DocumentedException {

        //TODO remove transactionProvider and CfgRegistry from parameters, accept only service ref store

        EditStrategyType editStrategyType = EditStrategyType.getDefaultStrategy();

        xml.checkName(EditConfigXmlParser.EDIT_CONFIG);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);


        XmlElement targetElement = null;
        XmlElement targetChildNode = null;
        targetElement  = xml.getOnlyChildElementWithSameNamespace(EditConfigXmlParser.TARGET_KEY);
        targetChildNode = targetElement.getOnlyChildElementWithSameNamespace();

        String datastoreValue = targetChildNode.getName();
        Datastore targetDatastore = Datastore.valueOf(datastoreValue);
        LOG.debug("Setting {} to '{}'", EditConfigXmlParser.TARGET_KEY, targetDatastore);

        // check target
        if (targetDatastore != Datastore.candidate){
            throw new DocumentedException(String.format(
                    "Only %s datastore supported for edit config but was: %s",
                    Datastore.candidate,
                    targetDatastore),
                    DocumentedException.ErrorType.application,
                    DocumentedException.ErrorTag.invalid_value,
                    DocumentedException.ErrorSeverity.error);
        }

        // Test option
        TestOption testOption;
        Optional<XmlElement> testOptionElementOpt = xml
                .getOnlyChildElementWithSameNamespaceOptionally(EditConfigXmlParser.TEST_OPTION_KEY);
        if (testOptionElementOpt.isPresent()) {
            String testOptionValue = testOptionElementOpt.get().getTextContent();
            testOption = TestOption.getFromXmlName(testOptionValue);
        } else {
            testOption = TestOption.getDefault();
        }
        LOG.debug("Setting {} to '{}'", EditConfigXmlParser.TEST_OPTION_KEY, testOption);

        // Error option
        Optional<XmlElement> errorOptionElement = xml
                .getOnlyChildElementWithSameNamespaceOptionally(EditConfigXmlParser.ERROR_OPTION_KEY);
        if (errorOptionElement.isPresent()) {
            String errorOptionParsed = errorOptionElement.get().getTextContent();
            if (!errorOptionParsed.equals(EditConfigXmlParser.DEFAULT_ERROR_OPTION)){
                throw new UnsupportedOperationException("Only " + EditConfigXmlParser.DEFAULT_ERROR_OPTION
                        + " supported for " + EditConfigXmlParser.ERROR_OPTION_KEY + ", was " + errorOptionParsed);
            }
        }

        // Default op
        Optional<XmlElement> defaultContent = xml
                .getOnlyChildElementWithSameNamespaceOptionally(EditConfigXmlParser.DEFAULT_OPERATION_KEY);
        if (defaultContent.isPresent()) {
            String mergeStrategyString = defaultContent.get().getTextContent();
            LOG.trace("Setting merge strategy to {}", mergeStrategyString);
            editStrategyType = EditStrategyType.valueOf(mergeStrategyString);
        }

        XmlElement configElement = null;
        configElement = xml.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.CONFIG_KEY);

        return new ConfigExecution(cfgMapping, configElement, testOption, editStrategyType);
    }
}
