/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.apache.aries.util.AriesFrameworkUtil;
import org.opendaylight.controller.blueprint.ext.OpendaylightNamespaceHandler;
import org.opendaylight.controller.config.api.ConfigSystemService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.EventConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is created in bundle activation and scans ACTIVE bundles for blueprint XML files located under
 * the well-known org/opendaylight/blueprint/ path and deploys the XML files via the Aries
 * BlueprintExtenderService. This path differs from the standard OSGI-INF/blueprint path to allow for
 * controlled deployment of blueprint containers in an orderly manner.
 *
 * @author Thomas Pantelis
 */
public class BlueprintBundleTracker implements BundleActivator, BundleTrackerCustomizer<Bundle>, EventHandler,
        SynchronousBundleListener {
    private static final Logger LOG = LoggerFactory.getLogger(BlueprintBundleTracker.class);
    private static final String BLUEPRINT_FILE_PATH = "org/opendaylight/blueprint/";
    private static final String BLUEPRINT_FLE_PATTERN = "*.xml";
    private static final long SYSTEM_BUNDLE_ID = 0;

    private ServiceTracker<BlueprintExtenderService, BlueprintExtenderService> serviceTracker;
    private BundleTracker<Bundle> bundleTracker;
    private BundleContext bundleContext;
    private volatile BlueprintExtenderService blueprintExtenderService;
    private volatile ServiceRegistration<?> blueprintContainerRestartReg;
    private volatile BlueprintContainerRestartServiceImpl restartService;
    private volatile boolean shuttingDown;
    private ServiceRegistration<?> eventHandlerReg;
    private ServiceRegistration<?> namespaceReg;

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void start(BundleContext context) {
        LOG.info("Starting {}", getClass().getSimpleName());

        bundleContext = context;

        registerBlueprintEventHandler(context);

        registerNamespaceHandler(context);

        bundleTracker = new BundleTracker<>(context, Bundle.ACTIVE, this);

        serviceTracker = new ServiceTracker<>(context, BlueprintExtenderService.class.getName(),
                new ServiceTrackerCustomizer<BlueprintExtenderService, BlueprintExtenderService>() {
                    @Override
                    public BlueprintExtenderService addingService(
                            ServiceReference<BlueprintExtenderService> reference) {
                        blueprintExtenderService = reference.getBundle().getBundleContext().getService(reference);
                        bundleTracker.open();

                        context.addBundleListener(BlueprintBundleTracker.this);

                        LOG.debug("Got BlueprintExtenderService");

                        restartService = new BlueprintContainerRestartServiceImpl(blueprintExtenderService);
                        blueprintContainerRestartReg = context.registerService(
                                BlueprintContainerRestartService.class.getName(), restartService, new Hashtable<>());

                        return blueprintExtenderService;
                    }

                    @Override
                    public void modifiedService(ServiceReference<BlueprintExtenderService> reference,
                            BlueprintExtenderService service) {
                    }

                    @Override
                    public void removedService(ServiceReference<BlueprintExtenderService> reference,
                            BlueprintExtenderService service) {
                    }
                });
        serviceTracker.open();
    }

    private void registerNamespaceHandler(BundleContext context) {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("osgi.service.blueprint.namespace", OpendaylightNamespaceHandler.NAMESPACE_1_0_0);
        namespaceReg = context.registerService(NamespaceHandler.class.getName(),
                new OpendaylightNamespaceHandler(), props);
    }

    private void registerBlueprintEventHandler(BundleContext context) {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(org.osgi.service.event.EventConstants.EVENT_TOPIC, EventConstants.TOPIC_CREATED);
        eventHandlerReg = context.registerService(EventHandler.class.getName(), this, props);
    }

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void stop(BundleContext context) {
        bundleTracker.close();
        serviceTracker.close();

        AriesFrameworkUtil.safeUnregisterService(eventHandlerReg);
        AriesFrameworkUtil.safeUnregisterService(namespaceReg);
        AriesFrameworkUtil.safeUnregisterService(blueprintContainerRestartReg);
    }

    /**
     * Implemented from SynchronousBundleListener.
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        // If the system bundle (id 0) is stopping, do an orderly shutdown of all blueprint containers. On
        // shutdown the system bundle is stopped first.
        if(event.getBundle().getBundleId() == SYSTEM_BUNDLE_ID && event.getType() == BundleEvent.STOPPING) {
            shutdownAllContainers();
        }
    }

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        modifiedBundle(bundle, event, bundle);
        return bundle;
    }

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        if(shuttingDown) {
            return;
        }

        if(bundle.getState() == Bundle.ACTIVE) {
            List<Object> paths = findBlueprintPaths(bundle);

            if(!paths.isEmpty()) {
                LOG.info("Creating blueprint container for bundle {} with paths {}", bundle, paths);

                blueprintExtenderService.createContainer(bundle, paths);
            }
        }
    }

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        // BlueprintExtenderService will handle this.
    }

    /**
     * Implemented from EventHandler to listen for blueprint events.
     *
     * @param event
     */
    @Override
    public void handleEvent(Event event) {
        if(EventConstants.TOPIC_CREATED.equals(event.getTopic())) {
            LOG.info("Blueprint container for bundle {} was successfully created",
                    event.getProperty(EventConstants.BUNDLE));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static List<Object> findBlueprintPaths(Bundle bundle) {
        Enumeration<?> e = bundle.findEntries(BLUEPRINT_FILE_PATH, BLUEPRINT_FLE_PATTERN, false);
        if(e == null) {
            return Collections.emptyList();
        } else {
            return Collections.list((Enumeration)e);
        }
    }

    private void shutdownAllContainers() {
        shuttingDown = true;

        restartService.close();

        // Close all CSS modules first.
        ConfigSystemService configSystem = getOSGiService(ConfigSystemService.class);
        if(configSystem != null) {
            configSystem.closeAllConfigModules();
        }

        LOG.info("Shutting down all blueprint containers...");

        Collection<Bundle> containerBundles = new HashSet<>(Arrays.asList(bundleContext.getBundles()));
        while(!containerBundles.isEmpty()) {
            // For each iteration of getBundlesToDestroy, as containers are destroyed, other containers become
            // eligible to be destroyed. We loop until we've destroyed them all.
            for(Bundle bundle : getBundlesToDestroy(containerBundles)) {
                containerBundles.remove(bundle);
                BlueprintContainer container = blueprintExtenderService.getContainer(bundle);
                if(container != null) {
                    blueprintExtenderService.destroyContainer(bundle, container);
                }
            }
        }

        LOG.info("Shutdown of blueprint containers complete");
    }

    private List<Bundle> getBundlesToDestroy(Collection<Bundle> containerBundles) {
        List<Bundle> bundlesToDestroy = new ArrayList<Bundle>();

        // Find all container bundles that either have no registered services or whose services are no
        // longer in use.
        for(Bundle bundle : containerBundles) {
            ServiceReference<?>[] references = bundle.getRegisteredServices();
            int usage = 0;
            if(references != null) {
                for(ServiceReference<?> reference : references) {
                    usage += getServiceUsage(reference);
                }
            }

            LOG.debug("Usage for bundle {} is {}", bundle, usage);
            if(usage == 0) {
                bundlesToDestroy.add(bundle);
            }
        }

        if(!bundlesToDestroy.isEmpty()) {
            Collections.sort(bundlesToDestroy, new Comparator<Bundle>() {
                @Override
                public int compare(Bundle b1, Bundle b2) {
                    return (int) (b2.getLastModified() - b1.getLastModified());
                }
            });

            LOG.debug("Selected bundles {} for destroy (no services in use)", bundlesToDestroy);
        } else {
            // There's either no more container bundles or they all have services being used. For
            // the latter it means there's either circular service usage or a service is being used
            // by a non-container bundle. But we need to make progress so we pick the bundle with a
            // used service with the highest service ID. Each service is assigned a monotonically
            // increasing ID as they are registered. By picking the bundle with the highest service
            // ID, we're picking the bundle that was (likely) started after all the others and thus
            // is likely the safest to destroy at this point.

            ServiceReference<?> ref = null;
            for(Bundle bundle : containerBundles) {
                ServiceReference<?>[] references = bundle.getRegisteredServices();
                if(references == null) {
                    continue;
                }

                for(ServiceReference<?> reference : references) {
                    // We did check the service usage above but it's possible the usage has changed since
                    // then,
                    if(getServiceUsage(reference) == 0) {
                        continue;
                    }

                    // Choose 'reference' if it has a lower service ranking or, if the rankings are equal
                    // which is usually the case, if it has a higher service ID. For the latter the < 0
                    // check looks backwards but that's how ServiceReference#compareTo is documented to work.
                    if(ref == null || reference.compareTo(ref) < 0) {
                        LOG.debug("Currently selecting bundle {} for destroy (with reference {})", bundle, reference);
                        ref = reference;
                    }
                }
            }

            if(ref != null) {
                bundlesToDestroy.add(ref.getBundle());
            }

            LOG.debug("Selected bundle {} for destroy (lowest ranking service or highest service ID)",
                    bundlesToDestroy);
        }

        return bundlesToDestroy;
    }

    private static int getServiceUsage(ServiceReference<?> ref) {
        Bundle[] usingBundles = ref.getUsingBundles();
        return usingBundles != null ? usingBundles.length : 0;
    }

    private <T> T getOSGiService(Class<T> serviceInterface) {
        try {
            ServiceReference<T> serviceReference = bundleContext.getServiceReference(serviceInterface);
            if(serviceReference == null) {
                LOG.warn("{} service reference not found", serviceInterface.getSimpleName());
                return null;
            }

            T service = bundleContext.getService(serviceReference);
            if(service == null) {
                // This could happen on shutdown if the service was already unregistered so we log as debug.
                LOG.debug("{} service instance was not found", serviceInterface.getSimpleName());
            }

            return service;
        } catch(IllegalStateException e) {
            // This is thrown if the BundleContext is no longer valid which is possible on shutdown so we
            // log as debug.
            LOG.debug("Error obtaining OSGi service {}", serviceInterface.getSimpleName(), e);
        }

        return null;
    }
}
