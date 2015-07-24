/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.Closeable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.facade.xml.mapping.IdentityMapping;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.facade.xml.mapping.config.InstanceConfig;
import org.opendaylight.controller.config.facade.xml.mapping.config.InstanceConfigElementResolved;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleConfig;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleElementDefinition;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.facade.xml.mapping.config.Services;
import org.opendaylight.controller.config.facade.xml.osgi.YangStoreContext;
import org.opendaylight.controller.config.facade.xml.osgi.YangStoreService;
import org.opendaylight.controller.config.facade.xml.runtime.InstanceRuntime;
import org.opendaylight.controller.config.facade.xml.runtime.ModuleRuntime;
import org.opendaylight.controller.config.facade.xml.runtime.Runtime;
import org.opendaylight.controller.config.facade.xml.strategy.EditConfigStrategy;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.facade.xml.transactions.TransactionProvider;
import org.opendaylight.controller.config.util.BeanReader;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorSeverity;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorTag;
import org.opendaylight.controller.config.util.xml.DocumentedException.ErrorType;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Config subsystem facade for xml format
 * <p/>
 * TODO extract generic interface for config subsystem facades
 */
public class ConfigSubsystemFacade implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigSubsystemFacade.class);
    private final YangStoreService yangStoreService;
    private final TransactionProvider transactionProvider;
    private final ConfigRegistryClient configRegistryClient;
    private final ConfigRegistryClient configRegistryClientNoNotifications;
    private final RpcFacade rpcFacade;

    public ConfigSubsystemFacade(ConfigRegistryClient configRegistryClient, ConfigRegistryClient configRegistryClientNoNotifications, YangStoreService yangStoreService, String id) {
        this.configRegistryClient = configRegistryClient;
        this.configRegistryClientNoNotifications = configRegistryClientNoNotifications;
        this.yangStoreService = yangStoreService;
        this.transactionProvider = new TransactionProvider(configRegistryClient, id);
        rpcFacade = new RpcFacade(yangStoreService, configRegistryClient);
    }

    public ConfigSubsystemFacade(ConfigRegistryClient configRegistryClient, ConfigRegistryClient configRegistryClientNoNotifications, YangStoreService yangStoreService, TransactionProvider txProvider) {
        this.configRegistryClient = configRegistryClient;
        this.configRegistryClientNoNotifications = configRegistryClientNoNotifications;
        this.yangStoreService = yangStoreService;
        this.transactionProvider = txProvider;
        rpcFacade = new RpcFacade(yangStoreService, configRegistryClient);
    }

    public Element getConfiguration(final Document document, final Datastore source, final Optional<String> maybeNamespace) {

        final ConfigTransactionClient registryClient;
        // Read current state from a transaction, if running is source, then start new transaction just for reading
        // in case of candidate, get current transaction representing candidate
        if (source == Datastore.running) {
            final ObjectName readTx = transactionProvider.getOrCreateReadTransaction();
            registryClient = configRegistryClient.getConfigTransactionClient(readTx);
        } else {
            registryClient = configRegistryClient.getConfigTransactionClient(transactionProvider.getOrCreateTransaction());
        }

        try {
            Element dataElement = XmlUtil.createElement(document, XmlMappingConstants.DATA_KEY, Optional.<String>absent());
            final Set<ObjectName> instances = Datastore.getInstanceQueryStrategy(source, this.transactionProvider)
                    .queryInstances(configRegistryClient);

            final Config configMapping =
                    new Config(transformMbeToModuleConfigs(yangStoreService.getModuleMXBeanEntryMap()), yangStoreService.getEnumResolver());

            ServiceRegistryWrapper serviceTracker = new ServiceRegistryWrapper(registryClient);
            dataElement = configMapping.toXml(instances, maybeNamespace, document, dataElement, serviceTracker);

            return dataElement;
        } finally {
            if (source == Datastore.running) {
                transactionProvider.closeReadTransaction();
            }
        }
    }

    public void executeConfigExecution(ConfigExecution configExecution) throws DocumentedException, ValidationException {
        if (configExecution.shouldTest()) {
            executeTests(configExecution);
        }

        if (configExecution.shouldSet()) {
            executeSet(configExecution);
        }
    }

    public CommitStatus commitTransaction() throws DocumentedException, ValidationException, ConflictingVersionException {
        final CommitStatus status = this.transactionProvider.commitTransaction();
        LOG.trace("Transaction committed successfully: {}", status);
        return status;
    }

    public CommitStatus commitSilentTransaction() throws DocumentedException, ValidationException, ConflictingVersionException {
        final CommitStatus status = this.transactionProvider.commitTransaction(configRegistryClientNoNotifications);
        LOG.trace("Transaction committed successfully: {}", status);
        return status;
    }

    private void executeSet(ConfigExecution configExecution) throws DocumentedException {
        set(configExecution);
        LOG.debug("Set phase for {} operation successful, element: ", configExecution.getDefaultStrategy(), configExecution.getConfigElement());
    }

    private void executeTests(ConfigExecution configExecution) throws DocumentedException, ValidationException {
        test(configExecution, configExecution.getDefaultStrategy());
        LOG.debug("Test phase for {} operation successful, element: ", configExecution.getDefaultStrategy(), configExecution.getConfigElement());
    }

    private void test(ConfigExecution execution, EditStrategyType editStrategyType) throws ValidationException, DocumentedException {
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

    private void set(ConfigExecution ConfigExecution) throws DocumentedException {
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
                        if (Services.ServiceInstance.EMPTY_SERVICE_INSTANCE == refNameToServiceEntry.getValue()) {
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

        for (Multimap<String, ModuleElementDefinition> modulesToResolved : execution.getModulesDefinition(ta).values()) {
            for (Map.Entry<String, ModuleElementDefinition> moduleToResolved : modulesToResolved.entries()) {
                String moduleName = moduleToResolved.getKey();

                ModuleElementDefinition moduleElementDefinition = moduleToResolved.getValue();

                EditConfigStrategy strategy = moduleElementDefinition.getEditStrategy();
                strategy.executeConfiguration(moduleName, moduleElementDefinition.getInstanceName(), null, ta, execution.getServiceRegistryWrapper(ta));
            }
        }
    }

    public Config getConfigMapping() {
        final YangStoreContext snapshot = yangStoreService.getCurrentSnapshot();
        Map<String, Map<String, ModuleConfig>> factories = transformMbeToModuleConfigs(snapshot.getModuleMXBeanEntryMap());
        Map<String, Map<Date, IdentityMapping>> identitiesMap = transformIdentities(snapshot.getModules());
        return new Config(factories, identitiesMap, snapshot.getEnumResolver());
    }

    private static Map<String, Map<Date, IdentityMapping>> transformIdentities(Set<Module> modules) {
        Map<String, Map<Date, IdentityMapping>> mappedIds = Maps.newHashMap();
        for (Module module : modules) {
            String namespace = module.getNamespace().toString();
            Map<Date, IdentityMapping> revisionsByNamespace = mappedIds.get(namespace);
            if (revisionsByNamespace == null) {
                revisionsByNamespace = Maps.newHashMap();
                mappedIds.put(namespace, revisionsByNamespace);
            }

            Date revision = module.getRevision();

            IdentityMapping identityMapping = revisionsByNamespace.get(revision);
            if (identityMapping == null) {
                identityMapping = new IdentityMapping();
                revisionsByNamespace.put(revision, identityMapping);
            }

            for (IdentitySchemaNode identitySchemaNode : module.getIdentities()) {
                identityMapping.addIdSchemaNode(identitySchemaNode);
            }

        }

        return mappedIds;
    }

    public Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleConfig>> transformMbeToModuleConfigs(
            Map<String/* Namespace from yang file */,
                    Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> mBeanEntries) {
        return transformMbeToModuleConfigs(configRegistryClient, mBeanEntries);
    }

    public Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleConfig>> transformMbeToModuleConfigs(BeanReader reader,
                                                                                                             Map<String/* Namespace from yang file */,
                                                                                                                     Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> mBeanEntries) {

        Map<String, Map<String, ModuleConfig>> namespaceToModuleNameToModuleConfig = Maps.newHashMap();

        for (Map.Entry<String, Map<String, ModuleMXBeanEntry>> namespaceToModuleToMbe : mBeanEntries.entrySet()) {
            for (Map.Entry<String, ModuleMXBeanEntry> moduleNameToMbe : namespaceToModuleToMbe.getValue().entrySet()) {
                String moduleName = moduleNameToMbe.getKey();
                ModuleMXBeanEntry moduleMXBeanEntry = moduleNameToMbe.getValue();

                ModuleConfig moduleConfig = new ModuleConfig(moduleName,
                        new InstanceConfig(reader, moduleMXBeanEntry.getAttributes(), moduleMXBeanEntry.getNullableDummyContainerName()));

                Map<String, ModuleConfig> moduleNameToModuleConfig = namespaceToModuleNameToModuleConfig.get(namespaceToModuleToMbe.getKey());
                if (moduleNameToModuleConfig == null) {
                    moduleNameToModuleConfig = Maps.newHashMap();
                    namespaceToModuleNameToModuleConfig.put(namespaceToModuleToMbe.getKey(), moduleNameToModuleConfig);
                }

                moduleNameToModuleConfig.put(moduleName, moduleConfig);
            }
        }

        return namespaceToModuleNameToModuleConfig;
    }

    public ConfigExecution getConfigExecution(final Config configMapping, final Element xmlToBePersisted) throws DocumentedException {
        return new ConfigExecution(configMapping, XmlElement.fromDomElement(xmlToBePersisted), TestOption.testThenSet, EditStrategyType.getDefaultStrategy());
    }

    private Map<String, Map<String, ModuleRuntime>> createModuleRuntimes(ConfigRegistryClient configRegistryClient,
                                                                         Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries) {
        Map<String, Map<String, ModuleRuntime>> retVal = Maps.newHashMap();

        for (Map.Entry<String, Map<String, ModuleMXBeanEntry>> namespaceToModuleEntry : mBeanEntries.entrySet()) {

            Map<String, ModuleRuntime> innerMap = Maps.newHashMap();
            Map<String, ModuleMXBeanEntry> entriesFromNamespace = namespaceToModuleEntry.getValue();
            for (Map.Entry<String, ModuleMXBeanEntry> moduleToMXEntry : entriesFromNamespace.entrySet()) {

                ModuleMXBeanEntry mbe = moduleToMXEntry.getValue();

                Map<RuntimeBeanEntry, InstanceConfig> cache = Maps.newHashMap();
                RuntimeBeanEntry root = null;
                for (RuntimeBeanEntry rbe : mbe.getRuntimeBeans()) {
                    cache.put(rbe, new InstanceConfig(configRegistryClient, rbe.getYangPropertiesToTypesMap(), mbe.getNullableDummyContainerName()));
                    if (rbe.isRoot()) {
                        root = rbe;
                    }
                }

                if (root == null) {
                    continue;
                }

                InstanceRuntime rootInstanceRuntime = createInstanceRuntime(root, cache);
                ModuleRuntime moduleRuntime = new ModuleRuntime(rootInstanceRuntime);
                innerMap.put(moduleToMXEntry.getKey(), moduleRuntime);
            }

            retVal.put(namespaceToModuleEntry.getKey(), innerMap);
        }
        return retVal;
    }

    private InstanceRuntime createInstanceRuntime(RuntimeBeanEntry root, Map<RuntimeBeanEntry, InstanceConfig> cache) {
        Map<String, InstanceRuntime> children = Maps.newHashMap();
        for (RuntimeBeanEntry child : root.getChildren()) {
            children.put(child.getJavaNamePrefix(), createInstanceRuntime(child, cache));
        }

        return new InstanceRuntime(cache.get(root), children, createJmxToYangMap(root.getChildren()));
    }

    private Map<String, String> createJmxToYangMap(List<RuntimeBeanEntry> children) {
        Map<String, String> jmxToYangNamesForChildRbe = Maps.newHashMap();
        for (RuntimeBeanEntry rbe : children) {
            jmxToYangNamesForChildRbe.put(rbe.getJavaNamePrefix(), rbe.getYangName());
        }
        return jmxToYangNamesForChildRbe;
    }

    public Element get(Document document) throws DocumentedException {
        final ObjectName testTransaction = transactionProvider.getOrCreateReadTransaction();
        final ConfigTransactionClient txClient = configRegistryClient.getConfigTransactionClient(testTransaction);

        try {
            // Runtime beans are not parts of transactions and have to be queried against the central registry
            final Set<ObjectName> runtimeBeans = configRegistryClient.lookupRuntimeBeans();

            final Set<ObjectName> configBeans = Datastore.getInstanceQueryStrategy(Datastore.running, transactionProvider)
                    .queryInstances(configRegistryClient);

            final Map<String, Map<String, ModuleRuntime>> moduleRuntimes = createModuleRuntimes(configRegistryClient,
                    yangStoreService.getModuleMXBeanEntryMap());

            final YangStoreContext yangStoreSnapshot = yangStoreService.getCurrentSnapshot();
            final Map<String, Map<String, ModuleConfig>> moduleConfigs = transformMbeToModuleConfigs(txClient,
                    yangStoreSnapshot.getModuleMXBeanEntryMap());

            final org.opendaylight.controller.config.facade.xml.runtime.Runtime runtime = new Runtime(moduleRuntimes, moduleConfigs);

            return runtime.toXml(runtimeBeans, configBeans, document, yangStoreSnapshot.getEnumResolver());
        } finally {
            transactionProvider.closeReadTransaction();
        }
    }

    public void abortConfiguration() {
        if (transactionProvider.getTransaction().isPresent()) {
            this.transactionProvider.abortTransaction();
        }
    }

    public void validateConfiguration() throws ValidationException {
        transactionProvider.validateTransaction();
    }

    @Override
    public void close() {
        transactionProvider.close();
    }

    public RpcFacade getRpcFacade() {
        return rpcFacade;
    }

}

