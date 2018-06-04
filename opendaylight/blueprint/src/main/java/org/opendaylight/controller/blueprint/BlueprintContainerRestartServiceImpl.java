/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the BlueprintContainerRestartService.
 *
 * @author Thomas Pantelis
 */
class BlueprintContainerRestartServiceImpl implements AutoCloseable, BlueprintContainerRestartService {
    private static final Logger LOG = LoggerFactory.getLogger(BlueprintContainerRestartServiceImpl.class);
    private static final int CONTAINER_CREATE_TIMEOUT_IN_MINUTES = 5;

    private final ExecutorService restartExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true).setNameFormat("BlueprintContainerRestartService").build());

    private BlueprintExtenderService blueprintExtenderService;
    private QuiesceParticipant quiesceParticipant;

    void setBlueprintExtenderService(final BlueprintExtenderService blueprintExtenderService) {
        this.blueprintExtenderService = blueprintExtenderService;
    }

    void setQuiesceParticipant(final QuiesceParticipant quiesceParticipant) {
        this.quiesceParticipant = quiesceParticipant;
    }

    public void restartContainer(final Bundle bundle, final List<Object> paths) {
        LOG.debug("restartContainer for bundle {}", bundle);

        if (restartExecutor.isShutdown()) {
            LOG.debug("Already closed - returning");
            return;
        }

        restartExecutor.execute(() -> {
            blueprintExtenderService.destroyContainer(bundle, blueprintExtenderService.getContainer(bundle));
            blueprintExtenderService.createContainer(bundle, paths);
        });
    }

    @Override
    public void restartContainerAndDependents(final Bundle bundle) {
        if (restartExecutor.isShutdown()) {
            return;
        }

        LOG.debug("restartContainerAndDependents for bundle {}", bundle);

        restartExecutor.execute(() -> restartContainerAndDependentsInternal(bundle));
    }

    private void restartContainerAndDependentsInternal(final Bundle forBundle) {
        Preconditions.checkNotNull(blueprintExtenderService);
        Preconditions.checkNotNull(quiesceParticipant);

        // We use a LinkedHashSet to preserve insertion order as we walk the service usage hierarchy.
        Set<Bundle> containerBundlesSet = new LinkedHashSet<>();
        findDependentContainersRecursively(forBundle, containerBundlesSet);

        List<Bundle> containerBundles = new ArrayList<>(containerBundlesSet);

        LOG.info("Restarting blueprint containers for bundle {} and its dependent bundles {}", forBundle,
                containerBundles.subList(1, containerBundles.size()));

        // The blueprint containers are created asynchronously so we register a handler for blueprint events
        // that are sent when a container is complete, successful or not. The CountDownLatch tells when all
        // containers are complete. This is done to ensure all blueprint containers are finished before we
        // restart config modules.
        final CountDownLatch containerCreationComplete = new CountDownLatch(containerBundles.size());
        ServiceRegistration<?> eventHandlerReg = registerEventHandler(forBundle.getBundleContext(), event -> {
            final Bundle bundle = event.getBundle();
            if (event.isReplay()) {
                LOG.trace("Got replay BlueprintEvent {} for bundle {}", event.getType(), bundle);
                return;
            }

            LOG.debug("Got BlueprintEvent {} for bundle {}", event.getType(), bundle);
            if (containerBundles.contains(bundle)
                    && (event.getType() == BlueprintEvent.CREATED || event.getType() == BlueprintEvent.FAILURE)) {
                containerCreationComplete.countDown();
                LOG.debug("containerCreationComplete is now {}", containerCreationComplete.getCount());
            }
        });

        final Runnable createContainerCallback = () -> createContainers(containerBundles);

        // Destroy the container down-top recursively and once done, restart the container top-down
        destroyContainers(new ArrayDeque<>(Lists.reverse(containerBundles)), createContainerCallback);


        try {
            if (!containerCreationComplete.await(CONTAINER_CREATE_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)) {
                LOG.warn("Failed to restart all blueprint containers within {} minutes. Attempted to restart {} {} "
                        + "but only {} completed restart", CONTAINER_CREATE_TIMEOUT_IN_MINUTES, containerBundles.size(),
                        containerBundles, containerBundles.size() - containerCreationComplete.getCount());
                return;
            }
        } catch (final InterruptedException e) {
            LOG.debug("CountDownLatch await was interrupted - returning");
            return;
        }

        AriesFrameworkUtil.safeUnregisterService(eventHandlerReg);

        LOG.info("Finished restarting blueprint containers for bundle {} and its dependent bundles", forBundle);
    }

    /**
     * Recursively quiesce and destroy the bundles one by one in order to maintain synchronicity and ordering.
     * @param remainingBundlesToDestroy the list of remaining bundles to destroy.
     * @param createContainerCallback a {@link Runnable} to {@code run()} when the recursive function is completed.
     */
    private void destroyContainers(final Deque<Bundle> remainingBundlesToDestroy,
            final Runnable createContainerCallback) {

        final Bundle nextBundle;
        synchronized (remainingBundlesToDestroy) {
            if (remainingBundlesToDestroy.isEmpty()) {
                LOG.debug("All blueprint containers were quiesced and destroyed");
                createContainerCallback.run();
                return;
            }

            nextBundle = remainingBundlesToDestroy.poll();
        }

        // The Quiesce capability is a like a soft-stop, clean-stop. In the case of the Blueprint extender, in flight
        // service calls are allowed to finish; they're counted in and counted out, and no new calls are allowed. When
        // there are no in flight service calls, the bundle is told to stop. The Blueprint bundle itself doesn't know
        // this is happening which is a key design point. In the case of Blueprint, the extender ensures no new Entity
        // Managers(EMs) are created. Then when all those EMs are closed the quiesce operation reports that it is
        // finished.
        // To properly restart the blueprint containers, first we have to quiesce the list of bundles, and once done, it
        // is safe to destroy their BlueprintContainer, so no reference is retained.
        //
        // Mail - thread explaining Quiesce API:
        //      https://www.mail-archive.com/dev@aries.apache.org/msg08403.html

        // Quiesced the bundle to unregister the associated BlueprintContainer
        quiesceParticipant.quiesce(bundlesQuiesced -> {

            // Destroy the container once Quiesced
            Arrays.stream(bundlesQuiesced).forEach(quiescedBundle -> {
                LOG.debug("Quiesced bundle {}", quiescedBundle);
                blueprintExtenderService.destroyContainer(
                        quiescedBundle, blueprintExtenderService.getContainer(quiescedBundle));
            });

            destroyContainers(remainingBundlesToDestroy, createContainerCallback);

        }, Collections.singletonList(nextBundle));
    }

    private void createContainers(final List<Bundle> containerBundles) {
        containerBundles.forEach(bundle -> {
            List<Object> paths = BlueprintBundleTracker.findBlueprintPaths(bundle);

            LOG.info("Restarting blueprint container for bundle {} with paths {}", bundle, paths);

            blueprintExtenderService.createContainer(bundle, paths);
        });
    }

    /**
     * Recursively finds the services registered by the given bundle and the bundles using those services.
     * User bundles that have an associated blueprint container are added to containerBundles.
     *
     * @param bundle the bundle to traverse
     * @param containerBundles the current set of bundles containing blueprint containers
     */
    private void findDependentContainersRecursively(final Bundle bundle, final Set<Bundle> containerBundles) {
        if (!containerBundles.add(bundle)) {
            // Already seen this bundle...
            return;
        }

        ServiceReference<?>[] references = bundle.getRegisteredServices();
        if (references != null) {
            for (ServiceReference<?> reference : references) {
                Bundle[] usingBundles = reference.getUsingBundles();
                if (usingBundles != null) {
                    for (Bundle usingBundle : usingBundles) {
                        if (blueprintExtenderService.getContainer(usingBundle) != null) {
                            findDependentContainersRecursively(usingBundle, containerBundles);
                        }
                    }
                }
            }
        }
    }

    private ServiceRegistration<?> registerEventHandler(final BundleContext bundleContext,
            final BlueprintListener listener) {
        return bundleContext.registerService(BlueprintListener.class.getName(), listener, new Hashtable<>());
    }

    @Override
    public void close() {
        LOG.debug("Closing");

        restartExecutor.shutdownNow();
    }
}
