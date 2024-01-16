/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = DataBrokerCommitExecutor.class,
    configurationPid = "org.opendaylight.controller.cluster.datastore.broker")
@Designate(ocd = DataBrokerCommitExecutor.Config.class)
public final class DataBrokerCommitExecutor {
    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(name = "max-data-broker-future-callback-queue-size")
        int callbackQueueSize() default 1000;
        @AttributeDefinition(name = "max-data-broker-future-callback-pool-size")
        int callbackPoolSize() default 20;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerCommitExecutor.class);

    private final DurationStatisticsTracker commitStatsTracker = DurationStatisticsTracker.createConcurrent();
    private final ThreadExecutorStatsMXBeanImpl threadStats;
    private final CommitStatsMXBeanImpl commitStats;
    private final ExecutorService executorService;

    @Activate
    public DataBrokerCommitExecutor(final Config config) {
        executorService = SpecialExecutors.newBlockingBoundedCachedThreadPool(config.callbackPoolSize(),
            config.callbackQueueSize(), "CommitFutures", ConcurrentDOMDataBroker.class);
        threadStats = ThreadExecutorStatsMXBeanImpl.create(executorService, "CommitFutureExecutorStats",
            "DOMDataBroker");
        commitStats = new CommitStatsMXBeanImpl(commitStatsTracker, "DOMDataBroker");
        commitStats.register();
        LOG.info("DOM Data Broker commit exector started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("DOM Data Broker commit exector stopping");
        commitStats.unregister();
        threadStats.unregister();
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("Future executor failed to finish in time, giving up", e);
        }
        LOG.info("DOM Data Broker commit exector stopped");
    }

    Executor executor() {
        return executorService;
    }

    DurationStatisticsTracker commitStatsTracker() {
        return commitStatsTracker;
    }
}
