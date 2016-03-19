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

/**
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

        for(int i = containerBundles.size() - 1; i >= 0; i--) {
            Bundle bundle = containerBundles.get(i);
            blueprintExtenderService.destroyContainer(bundle, blueprintExtenderService.getContainer(bundle));
        }

        for(Bundle bundle: containerBundles) {
            blueprintExtenderService.createContainer(bundle, BlueprintBundleTracker.findBlueprintPaths(bundle));
        }
    }

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
