/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.BoundedDequeBasedMailbox;
import akka.dispatch.MailboxType;
import akka.dispatch.ProducesMessageQueue;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.Config;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

public class MeteredBoundedMailbox implements MailboxType, ProducesMessageQueue<MeteredBoundedMailbox.MeteredMessageQueue> {

    private final Logger LOG = LoggerFactory.getLogger(MeteredBoundedMailbox.class);

    private MeteredMessageQueue queue;
    private final Integer capacity;
    private final FiniteDuration pushTimeOut;
    private final MetricRegistry registry;

    private final String QUEUE_SIZE = "q-size";

    public MeteredBoundedMailbox(ActorSystem.Settings settings, Config config) {

        CommonConfig commonConfig = new CommonConfig(settings.config());
        this.capacity = commonConfig.getMailBoxCapacity();
        this.pushTimeOut = commonConfig.getMailBoxPushTimeout();

        MetricsReporter reporter = MetricsReporter.getInstance(MeteringBehavior.DOMAIN);
        registry = reporter.getMetricsRegistry();
    }


    @Override
    public MeteredMessageQueue create(final scala.Option<ActorRef> owner, scala.Option<ActorSystem> system) {
        this.queue = new MeteredMessageQueue(this.capacity, this.pushTimeOut);
        monitorQueueSize(owner, this.queue);
        return this.queue;
    }

    private void monitorQueueSize(scala.Option<ActorRef> owner, final MeteredMessageQueue monitoredQueue) {
        if (owner.isEmpty()) {
            return; //there's no actor to monitor
        }
        String actorName = owner.get().path().toStringWithoutAddress();
        String metricName = MetricRegistry.name(actorName, QUEUE_SIZE);

        if (registry.getMetrics().containsKey(metricName))
        {
            return; //already registered
        }

        Gauge<Integer> queueSize = getQueueSizeGuage(monitoredQueue);
        registerQueueSizeMetric(metricName, queueSize);
    }


    public static class MeteredMessageQueue extends BoundedDequeBasedMailbox.MessageQueue {
        private static final long serialVersionUID = 1L;

        public MeteredMessageQueue(int capacity, FiniteDuration pushTimeOut) {
            super(capacity, pushTimeOut);
        }
    }

    private static Gauge<Integer> getQueueSizeGuage(final MeteredMessageQueue monitoredQueue ){
        return new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return monitoredQueue.size();
            }
        };
    }

    private void registerQueueSizeMetric(String metricName, Gauge<Integer> metric){
        try {
            registry.register(metricName,metric);
        } catch (IllegalArgumentException e) {
            LOG.warn("Unable to register queue size in metrics registry. Failed with exception {}. ", e);
        }
    }
}

