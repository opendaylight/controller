/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.Config;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.dispatch.BoundedDequeBasedMailbox;
import org.apache.pekko.dispatch.MailboxType;
import org.apache.pekko.dispatch.ProducesMessageQueue;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

public class MeteredBoundedMailbox implements MailboxType,
        ProducesMessageQueue<MeteredBoundedMailbox.MeteredMessageQueue> {
    private static final Logger LOG = LoggerFactory.getLogger(MeteredBoundedMailbox.class);
    private static final String QUEUE_SIZE = "q-size";

    private final Integer capacity;
    private final FiniteDuration pushTimeOut;

    public MeteredBoundedMailbox(final ActorSystem.Settings settings, final Config config) {

        CommonConfig commonConfig = new CommonConfig(settings.config());
        this.capacity = commonConfig.getMailBoxCapacity();
        this.pushTimeOut = commonConfig.getMailBoxPushTimeout();
    }


    @Override
    public MeteredMessageQueue create(final scala.Option<ActorRef> owner, final scala.Option<ActorSystem> system) {
        final MeteredMessageQueue queue = new MeteredMessageQueue(this.capacity, this.pushTimeOut);
        monitorQueueSize(owner, queue);
        return queue;
    }

    private static void monitorQueueSize(final scala.Option<ActorRef> owner, final MeteredMessageQueue monitoredQueue) {
        registerMetric(owner, QUEUE_SIZE, getQueueSizeGuage(monitoredQueue));
    }

    private static Gauge<Integer> getQueueSizeGuage(final MeteredMessageQueue monitoredQueue) {
        return monitoredQueue::size;
    }

    static <T extends Metric> void registerMetric(final scala.Option<ActorRef> owner, final String metricName,
            final T metric) {
        if (owner.isEmpty()) {
           // there's no actor to monitor
            return;
        }

        String actorName = owner.get().path().toStringWithoutAddress();
        String fullName = MetricRegistry.name(actorName, metricName);

        MetricRegistry registry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();

        if (registry.getMetrics().containsKey(fullName)) {
            // already registered
            return;
        }

        try {
            registry.register(fullName, metric);
        } catch (IllegalArgumentException e) {
            // already registered - shouldn't happen here since we check above...
            LOG.debug("Unable to register '{}' in metrics registry", fullName);
        }
    }

    public static class MeteredMessageQueue extends BoundedDequeBasedMailbox.MessageQueue {
        private static final long serialVersionUID = 1L;

        public MeteredMessageQueue(final int capacity, final FiniteDuration pushTimeOut) {
            super(capacity, pushTimeOut);
        }
    }
}
