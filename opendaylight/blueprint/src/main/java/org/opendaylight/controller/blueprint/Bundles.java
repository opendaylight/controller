/*
 * Copyright (c) 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bundles {
    private static final Logger LOG = LoggerFactory.getLogger(Bundles.class);

    private Bundles() {
        throw new AssertionError("Utility class");
    }

    static List<Bundle> getBundlesToDestroy(Collection<Bundle> containerBundles) {
        List<Bundle> bundlesToDestroy = new ArrayList<>();

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
}
