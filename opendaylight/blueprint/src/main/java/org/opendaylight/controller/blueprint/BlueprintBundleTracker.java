/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
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
public class BlueprintBundleTracker implements BundleActivator, BundleTrackerCustomizer<Bundle>, EventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BlueprintBundleTracker.class);
    private static final String BLUEPRINT_FILE_PATH = "org/opendaylight/blueprint/";
    private static final String BLUEPRINT_FLE_PATTERN = "*.xml";

    private ServiceTracker<BlueprintExtenderService, BlueprintExtenderService> serviceTracker;
    private BundleTracker<Bundle> bundleTracker;
    private volatile BlueprintExtenderService blueprintExtenderService;
    private ServiceRegistration<?> eventHandlerReg;

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void start(BundleContext context) {
        LOG.info("Starting {}", getClass().getSimpleName());

        // Register EventHandler for blueprint events

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(org.osgi.service.event.EventConstants.EVENT_TOPIC, EventConstants.TOPIC_CREATED);
        eventHandlerReg = context.registerService(EventHandler.class.getName(), this, props);

        bundleTracker = new BundleTracker<>(context, Bundle.ACTIVE, this);

        serviceTracker = new ServiceTracker<>(context, BlueprintExtenderService.class.getName(),
                new ServiceTrackerCustomizer<BlueprintExtenderService, BlueprintExtenderService>() {
                    @Override
                    public BlueprintExtenderService addingService(
                            ServiceReference<BlueprintExtenderService> reference) {
                        blueprintExtenderService = reference.getBundle().getBundleContext().getService(reference);
                        bundleTracker.open();

                        LOG.debug("Got BlueprintExtenderService");

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

    /**
     * Implemented from BundleActivator.
     */
    @Override
    public void stop(BundleContext context) {
        bundleTracker.close();
        serviceTracker.close();
        eventHandlerReg.unregister();
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
}
