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
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Config;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Datastore;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class EditConfigXmlParser {

    private static final Logger logger = LoggerFactory.getLogger(EditConfigXmlParser.class);

    public static final String EDIT_CONFIG = "edit-config";
    public static final String DEFAULT_OPERATION_KEY = "default-operation";
    static final String ERROR_OPTION_KEY = "error-option";
    static final String DEFAULT_ERROR_OPTION = "stop-on-error";
    static final String TARGET_KEY = "target";
    static final String TEST_OPTION_KEY = "test-option";

    public EditConfigXmlParser() {
    }

    EditConfigXmlParser.EditConfigExecution fromXml(final XmlElement xml, final Config cfgMapping,
                                                    TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient)
            throws NetconfDocumentedException {

        EditStrategyType editStrategyType = EditStrategyType.getDefaultStrategy();

        xml.checkName(EditConfigXmlParser.EDIT_CONFIG);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        XmlElement targetElement = xml.getOnlyChildElementWithSameNamespace(EditConfigXmlParser.TARGET_KEY);
        XmlElement targetChildNode = targetElement.getOnlyChildElementWithSameNamespace();
        String datastoreValue = targetChildNode.getName();
        Datastore targetDatastore = Datastore.valueOf(datastoreValue);
        logger.debug("Setting {} to '{}'", EditConfigXmlParser.TARGET_KEY, targetDatastore);

        // check target
        Preconditions.checkArgument(targetDatastore == Datastore.candidate,
                "Only %s datastore supported for edit config but was: %s", Datastore.candidate, targetDatastore);

        // Test option
        TestOption testOption;
        Optional<XmlElement> testOptionElementOpt = xml
                .getOnlyChildElementWithSameNamespaceOptionally(EditConfigXmlParser.TEST_OPTION_KEY);
        if (testOptionElementOpt.isPresent()) {
            String testOptionValue = testOptionElementOpt.get().getTextContent();
            testOption = EditConfigXmlParser.TestOption.getFromXmlName(testOptionValue);
        } else {
            testOption = EditConfigXmlParser.TestOption.getDefault();
        }
        logger.debug("Setting {} to '{}'", EditConfigXmlParser.TEST_OPTION_KEY, testOption);

        // Error option
        Optional<XmlElement> errorOptionElement = xml
                .getOnlyChildElementWithSameNamespaceOptionally(EditConfigXmlParser.ERROR_OPTION_KEY);
        if (errorOptionElement.isPresent()) {
            String errorOptionParsed = errorOptionElement.get().getTextContent();
            if (false == errorOptionParsed.equals(EditConfigXmlParser.DEFAULT_ERROR_OPTION))
                throw new UnsupportedOperationException("Only " + EditConfigXmlParser.DEFAULT_ERROR_OPTION
                        + " supported for " + EditConfigXmlParser.ERROR_OPTION_KEY + ", was " + errorOptionParsed);
        }

        // Default op
        Optional<XmlElement> defaultContent = xml
                .getOnlyChildElementWithSameNamespaceOptionally(EditConfigXmlParser.DEFAULT_OPERATION_KEY);
        if (defaultContent.isPresent()) {
            String mergeStrategyString = defaultContent.get().getTextContent();
            logger.trace("Setting merge strategy to {}", mergeStrategyString);
            editStrategyType = EditStrategyType.valueOf(mergeStrategyString);
        }
        Set<ObjectName> instancesForFillingServiceRefMapping = Collections.emptySet();
        if (editStrategyType == EditStrategyType.merge) {
            instancesForFillingServiceRefMapping = Datastore.getInstanceQueryStrategy(targetDatastore, transactionProvider)
                    .queryInstances(configRegistryClient);
            logger.trace("Pre-filling services from following instances: {}", instancesForFillingServiceRefMapping);
        }

        XmlElement configElement = xml.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.CONFIG_KEY);

        return new EditConfigXmlParser.EditConfigExecution(xml, cfgMapping, configElement, testOption,
                instancesForFillingServiceRefMapping, editStrategyType);
    }

    private void removeMountpointsFromConfig(XmlElement configElement, XmlElement mountpointsElement) {
        configElement.getDomElement().removeChild(mountpointsElement.getDomElement());
    }

    @VisibleForTesting
    static enum TestOption {
        testOnly, set, testThenSet;

        static TestOption getFromXmlName(String testOptionXmlName) {
            switch (testOptionXmlName) {
            case "test-only":
                return testOnly;
            case "test-then-set":
                return testThenSet;
            case "set":
                return set;
            default:
                throw new IllegalArgumentException("Unsupported test option " + testOptionXmlName + " supported: "
                        + Arrays.toString(TestOption.values()));
            }
        }

        public static TestOption getDefault() {
            return testThenSet;
        }

    }

    @VisibleForTesting
    static class EditConfigExecution {
        private final XmlElement editConfigXml;
        private final Map<String, Multimap<String, ModuleElementResolved>> resolvedXmlElements;
        private final TestOption testOption;
        private final EditStrategyType defaultEditStrategyType;

        EditConfigExecution(XmlElement xml, Config configResolver, XmlElement configElement, TestOption testOption, Set<ObjectName> instancesForFillingServiceRefMapping,
                            EditStrategyType defaultStrategy) {
            this.editConfigXml = xml;
            this.resolvedXmlElements = configResolver.fromXml(configElement, instancesForFillingServiceRefMapping, defaultStrategy);
            this.testOption = testOption;
            this.defaultEditStrategyType = defaultStrategy;
        }

        boolean shouldTest() {
            return testOption == TestOption.testOnly || testOption == TestOption.testThenSet;
        }

        boolean shouldSet() {
            return testOption == TestOption.set || testOption == TestOption.testThenSet;
        }

        Map<String, Multimap<String, ModuleElementResolved>> getResolvedXmlElements() {
            return resolvedXmlElements;
        }

        EditStrategyType getDefaultStrategy() {
            return defaultEditStrategyType;
        }
    }
}
