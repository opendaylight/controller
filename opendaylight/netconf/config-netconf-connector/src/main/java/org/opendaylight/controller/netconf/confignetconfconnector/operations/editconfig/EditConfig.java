/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementDefinition;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser.EditConfigExecution;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

        logger.trace("Operation {} successful", EditConfigXmlParser.EDIT_CONFIG);

        return document.createElement(XmlNetconfConstants.OK);
    }

    private void executeSet(ConfigRegistryClient configRegistryClient,
            EditConfigXmlParser.EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
        try {
            set(configRegistryClient, editConfigExecution);

        } catch (IllegalStateException e) {
            //FIXME: when can IllegalStateException be thrown?
            // JmxAttributeValidationException is wrapped in DynamicWritableWrapper with ValidationException
            // ValidationException is not thrown until validate or commit is issued
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
            test(configRegistryClient, editConfigExecution, editConfigExecution.getDefaultStrategy());
        } catch (IllegalStateException | ValidationException e) {
            //FIXME: when can IllegalStateException be thrown?
            logger.warn("Test phase for {} failed", EditConfigXmlParser.EDIT_CONFIG, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(), e.getMessage());
            throw new NetconfDocumentedException("Test phase: " + e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
        }
        logger.debug("Test phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void test(ConfigRegistryClient configRegistryClient, EditConfigExecution execution,
            EditStrategyType editStrategyType) throws ValidationException {
        ObjectName taON = transactionProvider.getTestTransaction();
        try {

            // default strategy = replace wipes config
            if (editStrategyType == EditStrategyType.replace) {
                transactionProvider.wipeTestTransaction(taON);
            }

            ConfigTransactionClient ta = configRegistryClient.getConfigTransactionClient(taON);

            handleMisssingInstancesOnTransaction(ta, execution);
            setServicesOnTransaction(ta, execution);
            setOnTransaction(ta, execution);
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

        ConfigTransactionClient ta = configRegistryClient.getConfigTransactionClient(taON);

        handleMisssingInstancesOnTransaction(ta, editConfigExecution);
        setServicesOnTransaction(ta, editConfigExecution);
        setOnTransaction(ta, editConfigExecution);
    }

    private void setServicesOnTransaction(ConfigTransactionClient ta, EditConfigExecution execution) {

        Services services = execution.getServices();

        Map<String, Map<String, Map<String, Services.ServiceInstance>>> namespaceToServiceNameToRefNameToInstance = services
                .getNamespaceToServiceNameToRefNameToInstance();

        for (String serviceNamespace : namespaceToServiceNameToRefNameToInstance.keySet()) {
            for (String serviceName : namespaceToServiceNameToRefNameToInstance.get(serviceNamespace).keySet()) {

                String qnameOfService = getQname(ta, serviceNamespace, serviceName);
                Map<String, Services.ServiceInstance> refNameToInstance = namespaceToServiceNameToRefNameToInstance
                        .get(serviceNamespace).get(serviceName);

                for (String refName : refNameToInstance.keySet()) {
                    ObjectName on = refNameToInstance.get(refName).getObjectName(ta.getTransactionName());
                    try {
                        ObjectName saved = ta.saveServiceReference(qnameOfService, refName, on);
                        logger.debug("Saving service {} with on {} under name {} with service on {}", qnameOfService,
                                on, refName, saved);
                    } catch (InstanceNotFoundException e) {
                        throw new IllegalStateException("Unable to save ref name " + refName + " for instance " + on, e);
                    }
                }
            }
        }
    }

    private String getQname(ConfigTransactionClient ta, String namespace, String serviceName) {
        return ta.getServiceInterfaceName(namespace, serviceName);
    }

    private void setOnTransaction(ConfigTransactionClient ta, EditConfigExecution execution) {

        for (Multimap<String, ModuleElementResolved> modulesToResolved : execution.getResolvedXmlElements(ta).values()) {

            for (Entry<String, ModuleElementResolved> moduleToResolved : modulesToResolved.entries()) {
                String moduleName = moduleToResolved.getKey();

                ModuleElementResolved moduleElementResolved = moduleToResolved.getValue();
                String instanceName = moduleElementResolved.getInstanceName();

                InstanceConfigElementResolved ice = moduleElementResolved.getInstanceConfigElementResolved();
                EditConfigStrategy strategy = ice.getEditStrategy();
                strategy.executeConfiguration(moduleName, instanceName, ice.getConfiguration(), ta, execution.getServiceRegistryWrapper(ta));
            }
        }
    }

    private void handleMisssingInstancesOnTransaction(ConfigTransactionClient ta,
            EditConfigExecution execution) {

        for (Multimap<String,ModuleElementDefinition> modulesToResolved : execution.getModulesDefinition(ta).values()) {
            for (Entry<String, ModuleElementDefinition> moduleToResolved : modulesToResolved.entries()) {
                String moduleName = moduleToResolved.getKey();

                ModuleElementDefinition moduleElementDefinition = moduleToResolved.getValue();

                EditConfigStrategy strategy = moduleElementDefinition.getEditStrategy();
                strategy.executeConfiguration(moduleName, moduleElementDefinition.getInstanceName(), null, ta, execution.getServiceRegistryWrapper(ta));
            }
        }
    }

    public static Config getConfigMapping(ConfigRegistryClient configRegistryClient, YangStoreSnapshot yangStoreSnapshot) {
        Map<String, Map<String, ModuleConfig>> factories = transformMbeToModuleConfigs(configRegistryClient,
                yangStoreSnapshot.getModuleMXBeanEntryMap());
        Map<String, Map<Date, IdentityMapping>> identitiesMap = transformIdentities(yangStoreSnapshot.getModules());
        return new Config(factories, identitiesMap);
    }


    public static class IdentityMapping {
        private final Map<String, IdentitySchemaNode> identityNameToSchemaNode;

        IdentityMapping() {
            this.identityNameToSchemaNode = Maps.newHashMap();
        }

        void addIdSchemaNode(IdentitySchemaNode node) {
            String name = node.getQName().getLocalName();
            Preconditions.checkState(identityNameToSchemaNode.containsKey(name) == false);
            identityNameToSchemaNode.put(name, node);
        }

        public boolean containsIdName(String idName) {
            return identityNameToSchemaNode.containsKey(idName);
        }

        // FIXME method never used
        public IdentitySchemaNode getIdentitySchemaNode(String idName) {
            Preconditions.checkState(identityNameToSchemaNode.containsKey(idName), "No identity under name %s", idName);
            return identityNameToSchemaNode.get(idName);
        }
    }

    private static Map<String, Map<Date, IdentityMapping>> transformIdentities(Set<Module> modules) {
        Map<String, Map<Date, IdentityMapping>> mappedIds = Maps.newHashMap();
        for (Module module : modules) {
            String namespace = module.getNamespace().toString();
            Map<Date, IdentityMapping> revisionsByNamespace= mappedIds.get(namespace);
            if(revisionsByNamespace == null) {
                revisionsByNamespace = Maps.newHashMap();
                mappedIds.put(namespace, revisionsByNamespace);
            }

            Date revision = module.getRevision();
            Preconditions.checkState(revisionsByNamespace.containsKey(revision) == false,
                    "Duplicate revision %s for namespace %s", revision, namespace);

            IdentityMapping identityMapping = revisionsByNamespace.get(revision);
            if(identityMapping == null) {
                identityMapping = new IdentityMapping();
                revisionsByNamespace.put(revision, identityMapping);
            }

            for (IdentitySchemaNode identitySchemaNode : module.getIdentities()) {
                identityMapping.addIdSchemaNode(identitySchemaNode);
            }

        }

        return mappedIds;
    }

    public static Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleConfig>> transformMbeToModuleConfigs
            (final ConfigRegistryClient configRegistryClient, Map<String/* Namespace from yang file */,
                    Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> mBeanEntries) {

        Map<String, Map<String, ModuleConfig>> namespaceToModuleNameToModuleConfig = Maps.newHashMap();

        for (String namespace : mBeanEntries.keySet()) {
            for (Entry<String, ModuleMXBeanEntry> moduleNameToMbe : mBeanEntries.get(namespace).entrySet()) {
                String moduleName = moduleNameToMbe.getKey();
                ModuleMXBeanEntry moduleMXBeanEntry = moduleNameToMbe.getValue();

                ModuleConfig moduleConfig = new ModuleConfig(moduleName, new InstanceConfig(configRegistryClient,
                        moduleMXBeanEntry.getAttributes()), moduleMXBeanEntry
                        .getProvidedServices().values());

                Map<String, ModuleConfig> moduleNameToModuleConfig = namespaceToModuleNameToModuleConfig.get(namespace);
                if(moduleNameToModuleConfig == null) {
                    moduleNameToModuleConfig = Maps.newHashMap();
                    namespaceToModuleNameToModuleConfig.put(namespace, moduleNameToModuleConfig);
                }

                moduleNameToModuleConfig.put(moduleName, moduleConfig);
            }
        }

        return namespaceToModuleNameToModuleConfig;
    }

    @Override
    protected String getOperationName() {
        return EditConfigXmlParser.EDIT_CONFIG;
    }

    @Override
    protected Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {

        EditConfigXmlParser.EditConfigExecution editConfigExecution;
        Config cfg = getConfigMapping(configRegistryClient, yangStoreSnapshot);
        try {
            editConfigExecution = editConfigXmlParser.fromXml(xml, cfg, transactionProvider, configRegistryClient);
        } catch (IllegalStateException e) {
            logger.warn("Error parsing xml", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.missing_attribute.name(), "Error parsing xml: " + e.getMessage());
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
