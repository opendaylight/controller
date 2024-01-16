/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.annotations.Beta;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.spi.ForwardingDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(service = DOMDataBroker.class, configurationPid = "org.opendaylight.controller.cluster.datastore.broker",
    property = "type=default")
@Designate(ocd = OSGiDOMDataBroker.Config.class)
public final class OSGiDOMDataBroker extends ForwardingDOMDataBroker {
    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(name = "max-data-broker-future-callback-queue-size")
        int callbackQueueSize() default 1000;
        @AttributeDefinition(name = "max-data-broker-future-callback-pool-size")
        int callbackPoolSize() default 20;
    }

    private static final Logger LOG = LoggerFactory.getLogger(OSGiDOMDataBroker.class);

    private final @NonNull ConcurrentDOMDataBroker delegate;
    private final ThreadExecutorStatsMXBeanImpl threadStats;
    private final CommitStatsMXBeanImpl commitStats;
    private final ExecutorService executorService;

    @Activate
    public OSGiDOMDataBroker(
            @Reference(target = "(type=distributed-config)") final DOMStore configDatastore,
            @Reference(target = "(type=distributed-operational)") final DOMStore operDatastore, final Config config) {
        LOG.info("DOM Data Broker starting");
        final var commitStatsTracker = DurationStatisticsTracker.createConcurrent();

        executorService = SpecialExecutors.newBlockingBoundedCachedThreadPool(config.callbackPoolSize(),
            config.callbackQueueSize(), "CommitFutures", ConcurrentDOMDataBroker.class);
        threadStats = ThreadExecutorStatsMXBeanImpl.create(executorService, "CommitFutureExecutorStats",
            "DOMDataBroker");
        commitStats = new CommitStatsMXBeanImpl(commitStatsTracker, "DOMDataBroker");
        commitStats.register();

        delegate = new ConcurrentDOMDataBroker(Map.of(
            LogicalDatastoreType.CONFIGURATION, configDatastore, LogicalDatastoreType.OPERATIONAL, operDatastore),
            executorService, commitStatsTracker);
        LOG.info("DOM Data Broker started");
    }

    @Override
    protected DOMDataBroker delegate() {
        return delegate;
    }

    @Deactivate
    void deactivate() {
        LOG.info("DOM Data Broker stopping");
        delegate.close();
        commitStats.unregister();
        threadStats.unregister();
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("Future executor failed to finish in time, giving up", e);
        }
        LOG.info("DOM Data Broker stopped");
    }
}
