/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Config;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfigElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser.EditConfigExecution;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EditConfig extends AbstractConfigNetconfOperation {

    private static final Logger logger = LoggerFactory.getLogger(EditConfig.class);

    private final YangStoreSnapshot yangStoreSnapshot;

    private final TransactionProvider transactionProvider;
    private EditConfigXmlParser editConfigXmlParser;

    public EditConfig(YangStoreSnapshot yangStoreSnapshot, TransactionProvider transactionProvider,
            ConfigRegistryClient configRegistryClient, String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.yangStoreSnapshot = yangStoreSnapshot;
        this.transactionProvider = transactionProvider;
        this.editConfigXmlParser = new EditConfigXmlParser();
    }

    @VisibleForTesting
    Element getResponseInternal(final Document document,
            final EditConfigXmlParser.EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
        if (editConfigExecution.shouldTest()) {
            executeTests(configRegistryClient, editConfigExecution);
        }

        if (editConfigExecution.shouldSet()) {
            executeSet(configRegistryClient, editConfigExecution);
        }

        logger.info("Operation {} successful", EditConfigXmlParser.EDIT_CONFIG);

        return document.createElement(XmlNetconfConstants.OK);
    }

    private void executeSet(ConfigRegistryClient configRegistryClient,
            EditConfigXmlParser.EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
        try {
            set(configRegistryClient, editConfigExecution);
        } catch (IllegalStateException | JmxAttributeValidationException | ValidationException e) {
            logger.warn("Set phase for {} failed", EditConfigXmlParser.EDIT_CONFIG, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(), e.getMessage());
            throw new NetconfDocumentedException("Test phase: " + e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
        }
        logger.debug("Set phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void executeTests(ConfigRegistryClient configRegistryClient,
            EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
        try {
            test(configRegistryClient, editConfigExecution.getResolvedXmlElements(), editConfigExecution.getDefaultStrategy());
        } catch (IllegalStateException | JmxAttributeValidationException | ValidationException e) {
            logger.warn("Test phase for {} failed", EditConfigXmlParser.EDIT_CONFIG, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(), e.getMessage());
            throw new NetconfDocumentedException("Test phase: " + e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
        }
        logger.debug("Test phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void test(ConfigRegistryClient configRegistryClient,
                      Map<String, Multimap<String, ModuleElementResolved>> resolvedModules, EditStrategyType editStrategyType) {
        ObjectName taON = transactionProvider.getTestTransaction();
        try {

            // default strategy = replace wipes config
            if (editStrategyType == EditStrategyType.replace) {
                transactionProvider.wipeTestTransaction(taON);
            }
            setOnTransaction(configRegistryClient, resolvedModules, taON);
            transactionProvider.validateTestTransaction(taON);
        } finally {
            transactionProvider.abortTestTransaction(taON);
        }
    }

    private void set(ConfigRegistryClient configRegistryClient,
            EditConfigXmlParser.EditConfigExecution editConfigExecution) {
        ObjectName taON = transactionProvider.getOrCreateTransaction();

        // default strategy = replace wipes config
        if (editConfigExecution.getDefaultStrategy() == EditStrategyType.replace) {
            transactionProvider.wipeTransaction();
        }
        setOnTransaction(configRegistryClient, editConfigExecution.getResolvedXmlElements(), taON);
    }

    private void setOnTransaction(ConfigRegistryClient configRegistryClient,
            Map<String, Multimap<String, ModuleElementResolved>> resolvedXmlElements, ObjectName taON) {
        ConfigTransactionClient ta = configRegistryClient.getConfigTransactionClient(taON);

        for (Multimap<String, ModuleElementResolved> modulesToResolved : resolvedXmlElements.values()) {
            for (Entry<String, ModuleElementResolved> moduleToResolved : modulesToResolved.entries()) {
                String moduleName = moduleToResolved.getKey();

                ModuleElementResolved moduleElementResolved = moduleToResolved.getValue();
                String instanceName = moduleElementResolved.getInstanceName();

                InstanceConfigElementResolved ice = moduleElementResolved.getInstanceConfigElementResolved();
                EditConfigStrategy strategy = ice.getEditStrategy();
                strategy.executeConfiguration(moduleName, instanceName, ice.getConfiguration(), ta);
            }
        }
    }

    public static Config getConfigMapping(ConfigRegistryClient configRegistryClient,
            Map<String/* Namespace from yang file */,
                    Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> mBeanEntries) {
        Map<String, Map<String, ModuleConfig>> factories = transform(configRegistryClient, mBeanEntries);

        return new Config(factories);
    }

    // TODO refactor
    private static Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleConfig>> transform
    (final ConfigRegistryClient configRegistryClient, Map<String/* Namespace from yang file */,
                    Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> mBeanEntries) {
        return Maps.transformEntries(mBeanEntries,
                new Maps.EntryTransformer<String, Map<String, ModuleMXBeanEntry>, Map<String, ModuleConfig>>() {

                    @Override
                    public Map<String, ModuleConfig> transformEntry(String arg0, Map<String, ModuleMXBeanEntry> arg1) {
                        return Maps.transformEntries(arg1,
                                new Maps.EntryTransformer<String, ModuleMXBeanEntry, ModuleConfig>() {

                                    @Override
                                    public ModuleConfig transformEntry(String key, ModuleMXBeanEntry moduleMXBeanEntry) {
                                        return new ModuleConfig(key, new InstanceConfig(configRegistryClient, moduleMXBeanEntry
                                                .getAttributes()), moduleMXBeanEntry.getProvidedServices().values());
                                    }
                                });
                    }
                });
    }

    @Override
    protected String getOperationName() {
        return EditConfigXmlParser.EDIT_CONFIG;
    }

    @Override
    protected Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {

        EditConfigXmlParser.EditConfigExecution editConfigExecution;
        Config cfg = getConfigMapping(configRegistryClient, yangStoreSnapshot.getModuleMXBeanEntryMap());
        try {
            editConfigExecution = editConfigXmlParser.fromXml(xml, cfg, transactionProvider, configRegistryClient);
        } catch (IllegalStateException e) {
            logger.warn("Error parsing xml", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.missing_attribute.name(), "Missing value for 'target' attribute");
            throw new NetconfDocumentedException(e.getMessage(), ErrorType.rpc, ErrorTag.missing_attribute,
                    ErrorSeverity.error, errorInfo);
        } catch (final IllegalArgumentException e) {
            logger.warn("Error parsing xml", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.bad_attribute.name(), e.getMessage());
            throw new NetconfDocumentedException(e.getMessage(), ErrorType.rpc, ErrorTag.bad_attribute,
                    ErrorSeverity.error, errorInfo);
        } catch (final UnsupportedOperationException e) {
            logger.warn("Unsupported", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_not_supported.name(), "Unsupported option for 'edit-config'");
            throw new NetconfDocumentedException(e.getMessage(), ErrorType.application,
                    ErrorTag.operation_not_supported, ErrorSeverity.error, errorInfo);
        }

        return getResponseInternal(document, editConfigExecution);
    }
}
