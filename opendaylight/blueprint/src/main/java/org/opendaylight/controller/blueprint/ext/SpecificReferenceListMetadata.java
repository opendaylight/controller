/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory metadata corresponding to the "specific-reference-list" element that obtains a specific list
 * of service instances from the OSGi registry for a given interface. The specific list is learned by first
 * extracting the list of expected service types by inspecting RESOLVED bundles for a resource file under
 * META-INF/services with the same name as the given interface. The type(s) listed in the resource file
 * must match the "type" property of the advertised service(s). In this manner, an app bundle announces the
 * service type(s) that it will advertise so that this class knows which services to expect up front. Once
 * all the expected services are obtained, the container is notified that all dependencies of this component
 * factory are satisfied.
 *
 * @author Thomas Pantelis
 */
class SpecificReferenceListMetadata extends AbstractDependentComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(SpecificReferenceListMetadata.class);

    private final String interfaceName;
    private final String serviceResourcePath;
    private final Collection<String> expectedServiceTypes = new ConcurrentSkipListSet<>();
    private final Collection<String> retrievedServiceTypes = new ConcurrentSkipListSet<>();
    private final Collection<Object> retrievedServices = Collections.synchronizedList(new ArrayList<>());
    private volatile BundleTracker<Bundle> bundleTracker;
    private volatile ServiceTracker<Object, Object> serviceTracker;

    SpecificReferenceListMetadata(String id, String interfaceName) {
        super(id);
        this.interfaceName = interfaceName;
        serviceResourcePath = "META-INF/services/" + interfaceName;
    }

    @Override
    protected void startTracking() {
        BundleTrackerCustomizer<Bundle> bundleListener = new BundleTrackerCustomizer<Bundle>() {
            @Override
            public Bundle addingBundle(Bundle bundle, BundleEvent event) {
                bundleAdded(bundle);
                return bundle;
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
            }
        };

        bundleTracker = new BundleTracker<>(container().getBundleContext(), Bundle.RESOLVED | Bundle.STARTING |
                Bundle.STOPPING | Bundle.ACTIVE, bundleListener);

        // This will get the list of all current RESOLVED+ bundles.
        bundleTracker.open();

        if(expectedServiceTypes.isEmpty()) {
            setSatisfied();
            return;
        }

        ServiceTrackerCustomizer<Object, Object> serviceListener = new ServiceTrackerCustomizer<Object, Object>() {
            @Override
            public Object addingService(ServiceReference<Object> reference) {
                return serviceAdded(reference);
            }

            @Override
            public void modifiedService(ServiceReference<Object> reference, Object service) {
            }

            @Override
            public void removedService(ServiceReference<Object> reference, Object service) {
                container().getBundleContext().ungetService(reference);
            }
        };

        setDependendencyDesc(interfaceName + " services with types " + expectedServiceTypes);

        serviceTracker = new ServiceTracker<>(container().getBundleContext(), interfaceName, serviceListener);
        serviceTracker.open();
    }

    private void bundleAdded(Bundle bundle) {
        URL resource = bundle.getEntry(serviceResourcePath);
        if(resource == null) {
            return;
        }

        LOG.debug("{}: Found {} resource in bundle {}", logName(), resource, bundle.getSymbolicName());

        try {
            for(String line : Resources.readLines(resource, StandardCharsets.UTF_8)) {
                int ci = line.indexOf('#');
                if(ci >= 0) {
                    line = line.substring(0, ci);
                }

                line = line.trim();
                if(line.isEmpty()) {
                    continue;
                }

                String serviceType = line;
                LOG.debug("{}: Retrieved service type {}", logName(), serviceType);
                expectedServiceTypes.add(serviceType);
            }
        } catch(IOException e) {
            setFailure(String.format("%s: Error reading resource %s from bundle %s", logName(), resource,
                    bundle.getSymbolicName()), e);
        }
    }

    private Object serviceAdded(ServiceReference<Object> reference) {
        Object service = container().getBundleContext().getService(reference);
        Object serviceType = reference.getProperty(OpendaylightNamespaceHandler.TYPE_ATTR);

        LOG.debug("{}: Service type {} added from bundle {}", logName(), serviceType,
                reference.getBundle().getSymbolicName());

        if(serviceType == null) {
            LOG.error("{}: Missing OSGi service property '{}' for service interface {} in bundle {}", logName(),
                    OpendaylightNamespaceHandler.TYPE_ATTR, interfaceName,  reference.getBundle().getSymbolicName());
            return service;
        }

        if(!expectedServiceTypes.contains(serviceType)) {
            LOG.error("{}: OSGi service property '{}' for service interface {} in bundle {} was not found in the " +
                    "expected service types {} obtained via {} bundle resources. Is the bundle resource missing or the service type misspelled?",
                    logName(), OpendaylightNamespaceHandler.TYPE_ATTR, interfaceName, reference.getBundle().getSymbolicName(),
                    expectedServiceTypes, serviceResourcePath);
            return service;
        }

        // If already satisfied, meaning we got all initial services, then a new bundle must've been
        // dynamically installed or a prior service's blueprint container was restarted, in which case we
        // restart our container.
        if(isSatisfied()) {
            restartContainer();
        } else {
            retrievedServiceTypes.add(serviceType.toString());
            retrievedServices.add(service);

            if(retrievedServiceTypes.equals(expectedServiceTypes)) {
                LOG.debug("{}: Got all expected service types", logName());
                setSatisfied();
            } else {
                Set<String> remaining = new HashSet<>(expectedServiceTypes);
                remaining.removeAll(retrievedServiceTypes);
                setDependendencyDesc(interfaceName + " services with types " + remaining);
            }
        }

        return service;
    }

    @Override
    public Object create() throws ComponentDefinitionException {
        LOG.debug("{}: In create: interfaceName: {}", logName(), interfaceName);

        super.onCreate();

        LOG.debug("{}: create returning service list {}", logName(), retrievedServices);

        synchronized(retrievedServices) {
            return ImmutableList.copyOf(retrievedServices);
        }
    }

    @Override
    public void destroy(Object instance) {
        super.destroy(instance);

        if(bundleTracker != null) {
            bundleTracker.close();
            bundleTracker = null;
        }

        if(serviceTracker != null) {
            serviceTracker.close();
            serviceTracker = null;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpecificReferenceListMetadata [interfaceName=").append(interfaceName)
                .append(", serviceResourcePath=").append(serviceResourcePath).append("]");
        return builder.toString();
    }
}
