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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.apache.aries.util.AriesFrameworkUtil;
import org.eclipse.jdt.annotation.Nullable;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.opendaylight.controller.blueprint.ext.OpendaylightNamespaceHandler;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
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
public class BlueprintBundleTracker implements BundleActivator, BundleTrackerCustomizer<Bundle>, BlueprintListener,
        SynchronousBundleListener {
    private static final Logger LOG = LoggerFactory.getLogger(BlueprintBundleTracker.class);
    private static final String ODL_CUSTOM_BLUEPRINT_FILE_PATH = "org/opendaylight/blueprint/";
    private static final String STANDARD_BLUEPRINT_FILE_PATH = "OSGI-INF/blueprint/";
    private static final String BLUEPRINT_FLE_PATTERN = "*.xml";
    private static final long SYSTEM_BUNDLE_ID = 0;

    private ServiceTracker<BlueprintExtenderService, BlueprintExtenderService> blueprintExtenderServiceTracker;
    private ServiceTracker<QuiesceParticipant, QuiesceParticipant> quiesceParticipantTracker;
    private BundleTracker<Bundle> bundleTracker;
    private BundleContext bundleContext;
    private volatile BlueprintExtenderService blueprintExtenderService;
    private volatile QuiesceParticipant quiesceParticipant;
    private volatile ServiceRegistration<?> blueprintContainerRestartReg;
    private volatile BlueprintContainerRestartServiceImpl restartService;
    private volatile boolean shuttingDown;
    private ServiceRegistration<?> eventHandlerReg;
    private ServiceRegistration<?> namespaceReg;

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void start(final BundleContext context) {
        LOG.info("Starting {}", getClass().getSimpleName());

        // CONTROLLER-1867: force UntrustedXML initialization, so that it uses our TCCL to initialize
        UntrustedXML.newDocumentBuilder();

        restartService = new BlueprintContainerRestartServiceImpl();

        bundleContext = context;

        registerBlueprintEventHandler(context);

        registerNamespaceHandler(context);

        bundleTracker = new BundleTracker<>(context, Bundle.ACTIVE, this);

        blueprintExtenderServiceTracker = new ServiceTracker<>(context, BlueprintExtenderService.class,
                new ServiceTrackerCustomizer<BlueprintExtenderService, BlueprintExtenderService>() {
                    @Override
                    public BlueprintExtenderService addingService(
                            final ServiceReference<BlueprintExtenderService> reference) {
                        return onBlueprintExtenderServiceAdded(reference);
                    }

                    @Override
                    public void modifiedService(final ServiceReference<BlueprintExtenderService> reference,
                            final BlueprintExtenderService service) {
                    }

                    @Override
                    public void removedService(final ServiceReference<BlueprintExtenderService> reference,
                            final BlueprintExtenderService service) {
                    }
                });
        blueprintExtenderServiceTracker.open();

        quiesceParticipantTracker = new ServiceTracker<>(context, QuiesceParticipant.class,
                new ServiceTrackerCustomizer<QuiesceParticipant, QuiesceParticipant>() {
                    @Override
                    public QuiesceParticipant addingService(
                            final ServiceReference<QuiesceParticipant> reference) {
                        return onQuiesceParticipantAdded(reference);
                    }

                    @Override
                    public void modifiedService(final ServiceReference<QuiesceParticipant> reference,
                                                final QuiesceParticipant service) {
                    }

                    @Override
                    public void removedService(final ServiceReference<QuiesceParticipant> reference,
                                               final QuiesceParticipant service) {
                    }
                });
        quiesceParticipantTracker.open();
    }

    private QuiesceParticipant onQuiesceParticipantAdded(final ServiceReference<QuiesceParticipant> reference) {
        quiesceParticipant = reference.getBundle().getBundleContext().getService(reference);

        LOG.debug("Got QuiesceParticipant");

        restartService.setQuiesceParticipant(quiesceParticipant);

        return quiesceParticipant;
    }

    private BlueprintExtenderService onBlueprintExtenderServiceAdded(
            final ServiceReference<BlueprintExtenderService> reference) {
        blueprintExtenderService = reference.getBundle().getBundleContext().getService(reference);
        bundleTracker.open();

        bundleContext.addBundleListener(BlueprintBundleTracker.this);

        LOG.debug("Got BlueprintExtenderService");

        restartService.setBlueprintExtenderService(blueprintExtenderService);

        blueprintContainerRestartReg = bundleContext.registerService(BlueprintContainerRestartService.class,
            restartService, null);

        return blueprintExtenderService;
    }

    private void registerNamespaceHandler(final BundleContext context) {
        Dictionary<String, Object> props = emptyDict();
        props.put("osgi.service.blueprint.namespace", OpendaylightNamespaceHandler.NAMESPACE_1_0_0);
        namespaceReg = context.registerService(NamespaceHandler.class, new OpendaylightNamespaceHandler(), props);
    }

    private void registerBlueprintEventHandler(final BundleContext context) {
        eventHandlerReg = context.registerService(BlueprintListener.class, this, null);
    }

    @SuppressModernizer
    private static Dictionary<String, Object> emptyDict() {
        return new Hashtable<>();
    }

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void stop(final BundleContext context) {
        bundleTracker.close();
        blueprintExtenderServiceTracker.close();
        quiesceParticipantTracker.close();

        AriesFrameworkUtil.safeUnregisterService(eventHandlerReg);
        AriesFrameworkUtil.safeUnregisterService(namespaceReg);
        AriesFrameworkUtil.safeUnregisterService(blueprintContainerRestartReg);
    }

    /**
     * Implemented from SynchronousBundleListener.
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        // If the system bundle (id 0) is stopping, do an orderly shutdown of all blueprint containers. On
        // shutdown the system bundle is stopped first.
        if (event.getBundle().getBundleId() == SYSTEM_BUNDLE_ID && event.getType() == BundleEvent.STOPPING) {
            shutdownAllContainers();
        }
    }

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
        modifiedBundle(bundle, event, bundle);
        return bundle;
    }

    /**
     * Implemented from BundleTrackerCustomizer.
     */
    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
        if (shuttingDown) {
            return;
        }

        if (bundle.getState() == Bundle.ACTIVE) {
            List<Object> paths = findBlueprintPaths(bundle, ODL_CUSTOM_BLUEPRINT_FILE_PATH);

            if (!paths.isEmpty()) {
                LOG.info("Creating blueprint container for bundle {} with paths {}", bundle, paths);

                blueprintExtenderService.createContainer(bundle, paths);
            }
        }
    }

    /**
     * Implemented from BundleTrackerCustomizer.
     */
    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
        // BlueprintExtenderService will handle this.
    }

    /**
     * Implemented from BlueprintListener to listen for blueprint events.
     *
     * @param event the event to handle
     */
    @Override
    public void blueprintEvent(final BlueprintEvent event) {
        if (event.getType() == BlueprintEvent.CREATED) {
            LOG.info("Blueprint container for bundle {} was successfully created", event.getBundle());
            return;
        }

        // If the container timed out waiting for dependencies, we'll destroy it and start it again. This
        // is indicated via a non-null DEPENDENCIES property containing the missing dependencies. The
        // default timeout is 5 min and ideally we would set this to infinite but the timeout can only
        // be set at the bundle level in the manifest - there's no way to set it globally.
        if (event.getType() == BlueprintEvent.FAILURE && event.getDependencies() != null) {
            Bundle bundle = event.getBundle();

            List<Object> paths = findBlueprintPaths(bundle);
            if (!paths.isEmpty()) {
                LOG.warn("Blueprint container for bundle {} timed out waiting for dependencies - restarting it",
                        bundle);

                restartService.restartContainer(bundle, paths);
            }
        }
    }

    static List<Object> findBlueprintPaths(final Bundle bundle) {
        List<Object> paths = findBlueprintPaths(bundle, STANDARD_BLUEPRINT_FILE_PATH);
        return !paths.isEmpty() ? paths : findBlueprintPaths(bundle, ODL_CUSTOM_BLUEPRINT_FILE_PATH);
    }

    private static List<Object> findBlueprintPaths(final Bundle bundle, final String path) {
        Enumeration<?> rntries = bundle.findEntries(path, BLUEPRINT_FLE_PATTERN, false);
        if (rntries == null) {
            return List.of();
        } else {
            return List.copyOf(Collections.list(rntries));
        }
    }

    private void shutdownAllContainers() {
        shuttingDown = true;

        restartService.close();

        LOG.info("Shutting down all blueprint containers...");

        Collection<Bundle> containerBundles = new HashSet<>(Arrays.asList(bundleContext.getBundles()));
        while (!containerBundles.isEmpty()) {
            // For each iteration of getBundlesToDestroy, as containers are destroyed, other containers become
            // eligible to be destroyed. We loop until we've destroyed them all.
            for (Bundle bundle : getBundlesToDestroy(containerBundles)) {
                containerBundles.remove(bundle);
                BlueprintContainer container = blueprintExtenderService.getContainer(bundle);
                if (container != null) {
                    blueprintExtenderService.destroyContainer(bundle, container);
                }
            }
        }

        LOG.info("Shutdown of blueprint containers complete");
    }

    private static List<Bundle> getBundlesToDestroy(final Collection<Bundle> containerBundles) {
        List<Bundle> bundlesToDestroy = new ArrayList<>();

        // Find all container bundles that either have no registered services or whose services are no
        // longer in use.
        for (Bundle bundle : containerBundles) {
            ServiceReference<?>[] references = bundle.getRegisteredServices();
            int usage = 0;
            if (references != null) {
                for (ServiceReference<?> reference : references) {
                    usage += getServiceUsage(reference);
                }
            }

            LOG.debug("Usage for bundle {} is {}", bundle, usage);
            if (usage == 0) {
                bundlesToDestroy.add(bundle);
            }
        }

        if (!bundlesToDestroy.isEmpty()) {
            bundlesToDestroy.sort((b1, b2) -> (int) (b2.getLastModified() - b1.getLastModified()));

            LOG.debug("Selected bundles {} for destroy (no services in use)", bundlesToDestroy);
        } else {
            // There's either no more container bundles or they all have services being used. For
            // the latter it means there's either circular service usage or a service is being used
            // by a non-container bundle. But we need to make progress so we pick the bundle with a
            // used service with the highest service ID. Each service is assigned a monotonically
            // increasing ID as they are registered. By picking the bundle with the highest service
            // ID, we're picking the bundle that was (likely) started after all the others and thus
            // is likely the safest to destroy at this point.

            Bundle bundle = findBundleWithHighestUsedServiceId(containerBundles);
            if (bundle != null) {
                bundlesToDestroy.add(bundle);
            }

            LOG.debug("Selected bundle {} for destroy (lowest ranking service or highest service ID)",
                    bundlesToDestroy);
        }

        return bundlesToDestroy;
    }

    private static @Nullable Bundle findBundleWithHighestUsedServiceId(final Collection<Bundle> containerBundles) {
        ServiceReference<?> highestServiceRef = null;
        for (Bundle bundle : containerBundles) {
            ServiceReference<?>[] references = bundle.getRegisteredServices();
            if (references == null) {
                continue;
            }

            for (ServiceReference<?> reference : references) {
                // We did check the service usage previously but it's possible the usage has changed since then.
                if (getServiceUsage(reference) == 0) {
                    continue;
                }

                // Choose 'reference' if it has a lower service ranking or, if the rankings are equal
                // which is usually the case, if it has a higher service ID. For the latter the < 0
                // check looks backwards but that's how ServiceReference#compareTo is documented to work.
                if (highestServiceRef == null || reference.compareTo(highestServiceRef) < 0) {
                    LOG.debug("Currently selecting bundle {} for destroy (with reference {})", bundle, reference);
                    highestServiceRef = reference;
                }
            }
        }

        return highestServiceRef == null ? null : highestServiceRef.getBundle();
    }

    private static int getServiceUsage(final ServiceReference<?> ref) {
        Bundle[] usingBundles = ref.getUsingBundles();
        return usingBundles != null ? usingBundles.length : 0;
    }
}
