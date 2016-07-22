/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.management.ManagementFactory;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.apache.aries.util.AriesFrameworkUtil;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.facade.xml.ConfigExecution;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.facade.xml.TestOption;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.EventConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of the BlueprintContainerRestartService.
 *
 * @author Thomas Pantelis
 */
class BlueprintContainerRestartServiceImpl implements AutoCloseable, BlueprintContainerRestartService {
    private static final Logger LOG = LoggerFactory.getLogger(BlueprintContainerRestartServiceImpl.class);
    private static final String CONFIG_MODULE_NAMESPACE_PROP = "config-module-namespace";
    private static final String CONFIG_MODULE_NAME_PROP = "config-module-name";
    private static final String CONFIG_INSTANCE_NAME_PROP = "config-instance-name";

    private final ExecutorService restartExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().
            setDaemon(true).setNameFormat("BlueprintContainerRestartService").build());
    private final BlueprintExtenderService blueprintExtenderService;

    BlueprintContainerRestartServiceImpl(BlueprintExtenderService blueprintExtenderService) {
        this.blueprintExtenderService = blueprintExtenderService;
    }

    public void restartContainer(final Bundle bundle, final List<Object> paths) {
        if(restartExecutor.isShutdown()) {
            return;
        }

        LOG.debug("restartContainer for bundle {}", bundle);

        restartExecutor.execute(new Runnable() {
            @Override
            public void run() {
                blueprintExtenderService.destroyContainer(bundle, blueprintExtenderService.getContainer(bundle));
                blueprintExtenderService.createContainer(bundle, paths);
            }
        });
    }

    @Override
    public void restartContainerAndDependents(final Bundle bundle) {
        if(restartExecutor.isShutdown()) {
            return;
        }

        LOG.debug("restartContainerAndDependents for bundle {}", bundle);

        restartExecutor.execute(new Runnable() {
            @Override
            public void run() {
                restartContainerAndDependentsInternal(bundle);

            }
        });
    }

    private void restartContainerAndDependentsInternal(Bundle forBundle) {
        // We use a LinkedHashSet to preserve insertion order as we walk the service usage hierarchy.
        Set<Bundle> containerBundlesSet = new LinkedHashSet<>();
        List<Entry<String, ModuleIdentifier>> configModules = new ArrayList<>();
        findDependentContainersRecursively(forBundle, containerBundlesSet, configModules);

        List<Bundle> containerBundles = new ArrayList<>(containerBundlesSet);

        LOG.info("Restarting blueprint containers for bundle {} and its dependent bundles {}", forBundle,
                containerBundles.subList(1, containerBundles.size()));

        // Destroy the containers in reverse order with 'forBundle' last, ie bottom-up in the service tree.
        for(Bundle bundle: Lists.reverse(containerBundles)) {
            blueprintExtenderService.destroyContainer(bundle, blueprintExtenderService.getContainer(bundle));
        }

        // The blueprint containers are created asynchronously so we register a handler for blueprint events
        // that are sent when a container is complete, successful or not. The CountDownLatch tells when all
        // containers are complete. This is done to ensure all blueprint containers are finished before we
        // restart config modules.
        final CountDownLatch containerCreationComplete = new CountDownLatch(containerBundles.size());
        ServiceRegistration<?> eventHandlerReg = registerEventHandler(forBundle.getBundleContext(), new EventHandler() {
            @Override
            public void handleEvent(Event event) {
                LOG.debug("handleEvent {} for bundle {}", event.getTopic(), event.getProperty(EventConstants.BUNDLE));
                if(containerBundles.contains(event.getProperty(EventConstants.BUNDLE))) {
                    containerCreationComplete.countDown();
                }
            }
        });

        // Restart the containers top-down starting with 'forBundle'.
        for(Bundle bundle: containerBundles) {
            List<Object> paths = BlueprintBundleTracker.findBlueprintPaths(bundle);

            LOG.info("Restarting blueprint container for bundle {} with paths {}", bundle, paths);

            blueprintExtenderService.createContainer(bundle, paths);
        }

        try {
            containerCreationComplete.await(5, TimeUnit.MINUTES);
        } catch(InterruptedException e) {
            LOG.debug("CountDownLatch await was interrupted - returning");
            return;
        }

        AriesFrameworkUtil.safeUnregisterService(eventHandlerReg);

        // Now restart any associated config system Modules.
        restartConfigModules(forBundle.getBundleContext(), configModules);
    }

    private void restartConfigModules(BundleContext bundleContext, List<Entry<String, ModuleIdentifier>> configModules) {
        if(configModules.isEmpty()) {
            return;
        }

        ServiceReference<ConfigSubsystemFacadeFactory> configFacadeFactoryRef =
                bundleContext.getServiceReference(ConfigSubsystemFacadeFactory.class);
        if(configFacadeFactoryRef == null) {
            LOG.debug("ConfigSubsystemFacadeFactory service reference not found");
            return;
        }

        ConfigSubsystemFacadeFactory configFacadeFactory = bundleContext.getService(configFacadeFactoryRef);
        if(configFacadeFactory == null) {
            LOG.debug("ConfigSubsystemFacadeFactory service not found");
            return;
        }

        ConfigSubsystemFacade configFacade = configFacadeFactory.createFacade("BlueprintContainerRestartService");
        try {
            restartConfigModules(configModules, configFacade);
        } catch(Exception e) {
            LOG.error("Error restarting config modules", e);
        } finally {
            configFacade.close();
            bundleContext.ungetService(configFacadeFactoryRef);
        }

    }

    private void restartConfigModules(List<Entry<String, ModuleIdentifier>> configModules,
            ConfigSubsystemFacade configFacade) throws ParserConfigurationException, DocumentedException,
                    ValidationException, ConflictingVersionException {

        Document document = XmlUtil.newDocument();
        Element dataElement = XmlUtil.createElement(document, XmlMappingConstants.DATA_KEY, Optional.<String>absent());
        Element modulesElement = XmlUtil.createElement(document, XmlMappingConstants.MODULES_KEY,
                Optional.of(XmlMappingConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG));
        dataElement.appendChild(modulesElement);

        Config configMapping = configFacade.getConfigMapping();

        ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory.getPlatformMBeanServer());
        for(Entry<String, ModuleIdentifier> entry: configModules) {
            String moduleNamespace = entry.getKey();
            ModuleIdentifier moduleId = entry.getValue();
            try {
                ObjectName instanceON = configRegistryClient.lookupConfigBean(moduleId.getFactoryName(),
                        moduleId.getInstanceName());

                LOG.debug("Found config module instance ObjectName: {}", instanceON);

                Element moduleElement = configMapping.moduleToXml(moduleNamespace, moduleId.getFactoryName(),
                        moduleId.getInstanceName(), instanceON, document);
                modulesElement.appendChild(moduleElement);
            } catch(InstanceNotFoundException e) {
                LOG.warn("Error looking up config module: namespace {}, module name {}, instance {}",
                        moduleNamespace, moduleId.getFactoryName(), moduleId.getInstanceName(), e);
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Pushing config xml: {}", XmlUtil.toString(dataElement));
        }

        ConfigExecution execution = new ConfigExecution(configMapping, XmlElement.fromDomElement(dataElement),
                TestOption.testThenSet, EditStrategyType.recreate);
        configFacade.executeConfigExecution(execution);
        configFacade.commitSilentTransaction();
    }

    /**
     * Recursively finds the services registered by the given bundle and the bundles using those services.
     * User bundles that have an associated blueprint container are added to containerBundles. In addition,
     * if a registered service has an associated config system Module, as determined via the presence of
     * certain service properties, the ModuleIdentifier is added to the configModules list.
     *
     * @param bundle the bundle to traverse
     * @param containerBundles the current set of bundles containing blueprint containers
     */
    private void findDependentContainersRecursively(Bundle bundle, Set<Bundle> containerBundles,
            List<Entry<String, ModuleIdentifier>> configModules) {
        if(!containerBundles.add(bundle)) {
            // Already seen this bundle...
            return;
        }

        ServiceReference<?>[] references = bundle.getRegisteredServices();
        if (references != null) {
            for (ServiceReference<?> reference : references) {
                possiblyAddConfigModuleIdentifier(reference, configModules);

                Bundle[] usingBundles = reference.getUsingBundles();
                if(usingBundles != null) {
                    for(Bundle usingBundle: usingBundles) {
                        if(blueprintExtenderService.getContainer(usingBundle) != null) {
                            findDependentContainersRecursively(usingBundle, containerBundles, configModules);
                        }
                    }
                }
            }
        }
    }

    private void possiblyAddConfigModuleIdentifier(ServiceReference<?> reference,
            List<Entry<String, ModuleIdentifier>> configModules) {
        Object moduleNamespace = reference.getProperty(CONFIG_MODULE_NAMESPACE_PROP);
        if(moduleNamespace == null) {
            return;
        }

        String moduleName = getRequiredConfigModuleProperty(CONFIG_MODULE_NAME_PROP, moduleNamespace,
                reference);
        String instanceName = getRequiredConfigModuleProperty(CONFIG_INSTANCE_NAME_PROP, moduleNamespace,
                reference);
        if(moduleName == null || instanceName == null) {
            return;
        }

        LOG.debug("Found service with config module: namespace {}, module name {}, instance {}",
                moduleNamespace, moduleName, instanceName);

        configModules.add(new SimpleEntry<>(moduleNamespace.toString(),
                new ModuleIdentifier(moduleName, instanceName)));
    }

    @Nullable
    private String getRequiredConfigModuleProperty(String propName, Object moduleNamespace,
            ServiceReference<?> reference) {
        Object value = reference.getProperty(propName);
        if(value == null) {
            LOG.warn("OSGi service with {} property is missing property {} therefore the config module can't be restarted",
                    CONFIG_MODULE_NAMESPACE_PROP, propName);
            return null;
        }

        return value.toString();
    }

    private ServiceRegistration<?> registerEventHandler(BundleContext bundleContext, EventHandler handler) {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(org.osgi.service.event.EventConstants.EVENT_TOPIC,
                new String[]{EventConstants.TOPIC_CREATED, EventConstants.TOPIC_FAILURE});
        return bundleContext.registerService(EventHandler.class.getName(), handler, props);
    }

    @Override
    public void close() {
        restartExecutor.shutdownNow();
    }
}
