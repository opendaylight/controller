/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of the BlueprintContainerRestartService.
 *
 * @author Thomas Pantelis
 */
class BlueprintContainerRestartServiceImpl implements AutoCloseable, BlueprintContainerRestartService {
    private static final Logger LOG = LoggerFactory.getLogger(BlueprintContainerRestartServiceImpl.class);

    private final ExecutorService restartExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().
            setDaemon(true).setNameFormat("BlueprintContainerRestartService").build());
    private final BlueprintExtenderService blueprintExtenderService;

    BlueprintContainerRestartServiceImpl(BlueprintExtenderService blueprintExtenderService) {
        this.blueprintExtenderService = blueprintExtenderService;
    }

    @Override
    public void restartContainerAndDependents(final Bundle bundle) {
        LOG.debug("restartContainerAndDependents for bundle {}", bundle);

        restartExecutor.execute(new Runnable() {
            @Override
            public void run() {
                restartContainerAndDependentsInternal(bundle);

            }
        });
    }

    private void restartContainerAndDependentsInternal(Bundle forBundle) {
        Set<Bundle> containerBundlesSet = new LinkedHashSet<>();
        findDependentContainersRecursively(forBundle, containerBundlesSet);

        List<Bundle> containerBundles = new ArrayList<>(containerBundlesSet);

        LOG.info("Restarting blueprint containers for bundle {} and its dependent bundles {}", forBundle,
                containerBundles.subList(1, containerBundles.size()));

        // Destroy the containers in reverse order with 'forBundle' last, ie bottom-up in the service tree.
        for(int i = containerBundles.size() - 1; i >= 0; i--) {
            Bundle bundle = containerBundles.get(i);
            blueprintExtenderService.destroyContainer(bundle, blueprintExtenderService.getContainer(bundle));
        }

        // Restart the containers top-down starting with 'forBundle'.
        for(Bundle bundle: containerBundles) {
            List<Object> paths = BlueprintBundleTracker.findBlueprintPaths(bundle);

            LOG.info("Restarting blueprint container for bundle {} with paths {}", bundle, paths);

            blueprintExtenderService.createContainer(bundle, paths);
        }
    }

    /**
     * Recursively finds the services registered by the given bundle and the bundles using those services.
     * User bundles that have an associated blueprint container are added to containerBundles.
     *
     * @param bundle the bundle to traverse
     * @param containerBundles the current set of bundles containing blueprint containers
     */
    private void findDependentContainersRecursively(Bundle bundle, Set<Bundle> containerBundles) {
        if(containerBundles.contains(bundle)) {
            return;
        }

        containerBundles.add(bundle);
        ServiceReference<?>[] references = bundle.getRegisteredServices();
        if (references != null) {
            for (ServiceReference<?> reference : references) {
                Bundle[] usingBundles = reference.getUsingBundles();
                if(usingBundles != null) {
                    for(Bundle usingBundle: usingBundles) {
                        if(blueprintExtenderService.getContainer(usingBundle) != null) {
                            findDependentContainersRecursively(usingBundle, containerBundles);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        restartExecutor.shutdownNow();
    }
}
