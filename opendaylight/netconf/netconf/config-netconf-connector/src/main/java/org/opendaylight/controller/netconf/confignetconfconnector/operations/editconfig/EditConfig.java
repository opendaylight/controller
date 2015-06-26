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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.persist.mapping.ConfigExecution;
import org.opendaylight.controller.config.persist.mapping.mapping.IdentityMapping;
import org.opendaylight.controller.config.persist.mapping.mapping.config.Config;
import org.opendaylight.controller.config.persist.mapping.mapping.config.InstanceConfig;
import org.opendaylight.controller.config.persist.mapping.mapping.config.InstanceConfigElementResolved;
import org.opendaylight.controller.config.persist.mapping.mapping.config.ModuleConfig;
import org.opendaylight.controller.config.persist.mapping.mapping.config.ModuleElementDefinition;
import org.opendaylight.controller.config.persist.mapping.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.config.persist.mapping.mapping.config.Services;
import org.opendaylight.controller.config.persist.mapping.mapping.config.Services.ServiceInstance;
import org.opendaylight.controller.config.persist.mapping.osgi.YangStoreContext;
import org.opendaylight.controller.config.persist.mapping.strategy.EditConfigStrategy;
import org.opendaylight.controller.config.persist.mapping.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.BeanReader;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorSeverity;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorTag;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorType;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
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
            final ConfigExecution ConfigExecution) throws DocumentedException {

        if (ConfigExecution.shouldTest()) {
            executeTests(getConfigRegistryClient(), ConfigExecution);
        }

        if (ConfigExecution.shouldSet()) {
            executeSet(getConfigRegistryClient(), ConfigExecution);
        }

        LOG.trace("Operation {} successful", EditConfigXmlParser.EDIT_CONFIG);

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    private void executeSet(ConfigRegistryClient configRegistryClient,
            ConfigExecution ConfigExecution) throws DocumentedException {
        set(configRegistryClient, ConfigExecution);
        LOG.debug("Set phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void executeTests(ConfigRegistryClient configRegistryClient,
            ConfigExecution ConfigExecution) throws DocumentedException {
        try {
            test(configRegistryClient, ConfigExecution, ConfigExecution.getDefaultStrategy());
        } catch (final ValidationException e) {
            LOG.warn("Test phase for {} failed", EditConfigXmlParser.EDIT_CONFIG, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(), e.getMessage());
            throw new DocumentedException("Test phase: " + e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_failed, ErrorSeverity.error, errorInfo);
        }
        LOG.debug("Test phase for {} operation successful", EditConfigXmlParser.EDIT_CONFIG);
    }

    private void test(ConfigRegistryClient configRegistryClient, ConfigExecution execution,
            EditStrategyType editStrategyType) throws ValidationException, DocumentedException {
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
            ConfigExecution ConfigExecution) throws DocumentedException {
        ObjectName taON = transactionProvider.getOrCreateTransaction();

        // default strategy = replace wipes config
        if (ConfigExecution.getDefaultStrategy() == EditStrategyType.replace) {
            transactionProvider.wipeTransaction();
        }

        ConfigTransactionClient ta = configRegistryClient.getConfigTransactionClient(taON);

        handleMisssingInstancesOnTransaction(ta, ConfigExecution);
        setServicesOnTransaction(ta, ConfigExecution);
        setOnTransaction(ta, ConfigExecution);
    }

    private void setServicesOnTransaction(ConfigTransactionClient ta, ConfigExecution execution) throws DocumentedException {

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
                        if (ServiceInstance.EMPTY_SERVICE_INSTANCE == refNameToServiceEntry.getValue()) {
                            ta.removeServiceReference(qnameOfService, refNameToServiceEntry.getKey());
                            LOG.debug("Removing service {} with name {}", qnameOfService, refNameToServiceEntry.getKey());
                        } else {
                            ObjectName saved = ta.saveServiceReference(qnameOfService, refNameToServiceEntry.getKey(), on);
                            LOG.debug("Saving service {} with on {} under name {} with service on {}", qnameOfService,
                                    on, refNameToServiceEntry.getKey(), saved);
                        }
                    } catch (InstanceNotFoundException e) {
                        throw new DocumentedException(String.format("Unable to edit ref name " + refNameToServiceEntry.getKey() + " for instance " + on, e),
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

    private void setOnTransaction(ConfigTransactionClient ta, ConfigExecution execution) throws DocumentedException {

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
            ConfigExecution execution) throws DocumentedException {

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
        return new Config(factories, identitiesMap, yangStoreSnapshot.getEnumResolver());
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
        Map<String /* Name of module entry from yang file */, ModuleConfig>> transformMbeToModuleConfigs (
            final BeanReader configRegistryClient, Map<String/* Namespace from yang file */,
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
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        ConfigExecution configExecution;
        Config cfg = getConfigMapping(getConfigRegistryClient(), yangStoreSnapshot);
        configExecution = editConfigXmlParser.fromXml(xml, cfg);

        Element responseInternal;
        responseInternal = getResponseInternal(document, configExecution);
        return responseInternal;
    }

}
