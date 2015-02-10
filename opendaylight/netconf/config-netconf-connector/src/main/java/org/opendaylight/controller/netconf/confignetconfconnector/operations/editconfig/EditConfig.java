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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Config;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfigElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementDefinition;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser.EditConfigExecution;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreContext;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EditConfig extends AbstractConfigNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(EditConfig.class);

    private final YangStoreContext yangStoreSnapshot;

    private final TransactionProvider transactionProvider;
    private EditConfigXmlParser editConfigXmlParser;

    public EditConfig(YangStoreContext yangStoreSnapshot, TransactionProvider transactionProvider,
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
            executeTests(getConfigRegistryClient(), editConfigExecution);
        }

        if (editConfigExecution.shouldSet()) {
            executeSet(getConfigRegistryClient(), editConfigExecution);
        }

        LOG.trace("Operation {} successful", EditConfigXmlParser.EDIT_CONFIG);

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    private void executeSet(ConfigRegistryClient configRegistryClient,
            EditConfigXmlParser.EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
        set(configRegistryClient, editConfigExecution);
        LOG.debug("Set phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void executeTests(ConfigRegistryClient configRegistryClient,
            EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
        try {
            test(configRegistryClient, editConfigExecution, editConfigExecution.getDefaultStrategy());
        } catch (final ValidationException e) {
            LOG.warn("Test phase for {} failed", EditConfigXmlParser.EDIT_CONFIG, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(), e.getMessage());
            throw new NetconfDocumentedException("Test phase: " + e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
        }
        LOG.debug("Test phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void test(ConfigRegistryClient configRegistryClient, EditConfigExecution execution,
            EditStrategyType editStrategyType) throws ValidationException, NetconfDocumentedException {
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
            EditConfigXmlParser.EditConfigExecution editConfigExecution) throws NetconfDocumentedException {
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

    private void setServicesOnTransaction(ConfigTransactionClient ta, EditConfigExecution execution) throws NetconfDocumentedException {

        Services services = execution.getServices();

        Map<String, Map<String, Map<String, Services.ServiceInstance>>> namespaceToServiceNameToRefNameToInstance = services
                .getNamespaceToServiceNameToRefNameToInstance();

        for (Map.Entry<String, Map<String, Map<String, Services.ServiceInstance>>> namespaceToServiceToRefEntry : namespaceToServiceNameToRefNameToInstance.entrySet()) {
            for (Map.Entry<String, Map<String, Services.ServiceInstance>> serviceToRefEntry : namespaceToServiceToRefEntry.getValue().entrySet()) {

                String qnameOfService = getQname(ta, namespaceToServiceToRefEntry.getKey(), serviceToRefEntry.getKey());
                Map<String, Services.ServiceInstance> refNameToInstance = serviceToRefEntry.getValue();

                for (Map.Entry<String, Services.ServiceInstance> refNameToServiceEntry : refNameToInstance.entrySet()) {
                    ObjectName on = refNameToServiceEntry.getValue().getObjectName(ta.getTransactionName());
                    try {
                        ObjectName saved = ta.saveServiceReference(qnameOfService, refNameToServiceEntry.getKey(), on);
                        LOG.debug("Saving service {} with on {} under name {} with service on {}", qnameOfService,
                                on, refNameToServiceEntry.getKey(), saved);
                    } catch (InstanceNotFoundException e) {
                        throw new NetconfDocumentedException(String.format("Unable to save ref name " + refNameToServiceEntry.getKey() + " for instance " + on, e),
                                ErrorType.application,
                                ErrorTag.operation_failed,
                                ErrorSeverity.error);
                    }
                }
            }
        }
    }

    private String getQname(ConfigTransactionClient ta, String namespace, String serviceName) {
        return ta.getServiceInterfaceName(namespace, serviceName);
    }

    private void setOnTransaction(ConfigTransactionClient ta, EditConfigExecution execution) throws NetconfDocumentedException {

        for (Multimap<String, ModuleElementResolved> modulesToResolved : execution.getResolvedXmlElements(ta).values()) {

            for (Map.Entry<String, ModuleElementResolved> moduleToResolved : modulesToResolved.entries()) {
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
            EditConfigExecution execution) throws NetconfDocumentedException {

        for (Multimap<String,ModuleElementDefinition> modulesToResolved : execution.getModulesDefinition(ta).values()) {
            for (Map.Entry<String, ModuleElementDefinition> moduleToResolved : modulesToResolved.entries()) {
                String moduleName = moduleToResolved.getKey();

                ModuleElementDefinition moduleElementDefinition = moduleToResolved.getValue();

                EditConfigStrategy strategy = moduleElementDefinition.getEditStrategy();
                strategy.executeConfiguration(moduleName, moduleElementDefinition.getInstanceName(), null, ta, execution.getServiceRegistryWrapper(ta));
            }
        }
    }

    public static Config getConfigMapping(ConfigRegistryClient configRegistryClient, YangStoreContext yangStoreSnapshot) {
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
            Preconditions.checkState(!identityNameToSchemaNode.containsKey(name));
            identityNameToSchemaNode.put(name, node);
        }

        public boolean containsIdName(String idName) {
            return identityNameToSchemaNode.containsKey(idName);
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
            Preconditions.checkState(!revisionsByNamespace.containsKey(revision),
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

        for (Map.Entry<String, Map<String, ModuleMXBeanEntry>> namespaceToModuleToMbe : mBeanEntries.entrySet()) {
            for (Map.Entry<String, ModuleMXBeanEntry> moduleNameToMbe : namespaceToModuleToMbe.getValue().entrySet()) {
                String moduleName = moduleNameToMbe.getKey();
                ModuleMXBeanEntry moduleMXBeanEntry = moduleNameToMbe.getValue();

                ModuleConfig moduleConfig = new ModuleConfig(moduleName,
                        new InstanceConfig(configRegistryClient,moduleMXBeanEntry.getAttributes(), moduleMXBeanEntry.getNullableDummyContainerName()));

                Map<String, ModuleConfig> moduleNameToModuleConfig = namespaceToModuleNameToModuleConfig.get(namespaceToModuleToMbe.getKey());
                if(moduleNameToModuleConfig == null) {
                    moduleNameToModuleConfig = Maps.newHashMap();
                    namespaceToModuleNameToModuleConfig.put(namespaceToModuleToMbe.getKey(), moduleNameToModuleConfig);
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
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {

        EditConfigXmlParser.EditConfigExecution editConfigExecution;
        Config cfg = getConfigMapping(getConfigRegistryClient(), yangStoreSnapshot);
        editConfigExecution = editConfigXmlParser.fromXml(xml, cfg);

        Element responseInternal;
        responseInternal = getResponseInternal(document, editConfigExecution);
        return responseInternal;
    }

}
