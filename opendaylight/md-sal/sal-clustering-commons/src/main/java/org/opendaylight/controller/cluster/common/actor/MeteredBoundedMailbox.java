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
import akka.dispatch.Envelope;
import akka.dispatch.MailboxType;
import akka.dispatch.ProducesMessageQueue;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.Config;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

public class MeteredBoundedMailbox implements MailboxType,
        ProducesMessageQueue<MeteredBoundedMailbox.MeteredMessageQueue> {
    private static final Logger LOG = LoggerFactory.getLogger(MeteredBoundedMailbox.class);

    private static final String QUEUE_SIZE        = "q-size";
    private static final String QUEUE_HANDLED     = "handled";
    private static final String QUEUE_NOT_HANDLED = "not-handled";


    private MeteredMessageQueue queue;
    private final Integer capacity;
    private final FiniteDuration pushTimeOut;
    private final MetricRegistry registry;

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
        monitorQueueMetrics(owner, this.queue);
        return this.queue;
    }

    private void monitorQueueMetrics(scala.Option<ActorRef> owner, final MeteredMessageQueue monitoredQueue) {
        if (owner.isEmpty()) {
            return; //there's no actor to monitor
        }
        String actorName = owner.get().path().toStringWithoutAddress();
        String sizeMetricName       = MetricRegistry.name(actorName, QUEUE_SIZE);
        String handledMetricName    = MetricRegistry.name(actorName, QUEUE_HANDLED);
        String notHandledMetricName = MetricRegistry.name(actorName, QUEUE_NOT_HANDLED);

        try {
            if (!registry.getMetrics().containsKey(sizeMetricName)) {
                registry.register(sizeMetricName, (Gauge<Integer>)(() -> monitoredQueue.size()));
            }
            if (!registry.getMetrics().containsKey(handledMetricName)) {
                registry.register(handledMetricName, (Gauge<Long>)(() -> monitoredQueue.getHandledMessageCount()));
            }
            if (!registry.getMetrics().containsKey(notHandledMetricName)) {
                registry.register(notHandledMetricName,
                                  (Gauge<Long>)(() -> monitoredQueue.getNotHandledMessageCount()));
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Unable to register {}, {} and/or {} in metrics registry for actor {} due to {}",
                     sizeMetricName, handledMetricName, notHandledMetricName, actorName, e);
        }
    }

    public static class MeteredMessageQueue extends BoundedDequeBasedMailbox.MessageQueue {
        private static final long serialVersionUID = 1L;
        private final AtomicLong handledMsgCnt     = new AtomicLong(0);
        private final AtomicLong notHandledMsgCnt  = new AtomicLong(0);

        public MeteredMessageQueue(int capacity, FiniteDuration pushTimeOut) {
            super(capacity, pushTimeOut);
        }

        public void enqueue(ActorRef receiver, Envelope handle) {
            super.enqueue(receiver, handle);
            notHandledMsgCnt.getAndIncrement();
        }

        public Envelope dequeue() {
            Envelope mail = super.dequeue();
            if (mail != null) {
                handledMsgCnt.getAndIncrement();
                notHandledMsgCnt.getAndDecrement();
            }
            return mail;
        }

        public long getHandledMessageCount() {
            return handledMsgCnt.get();
        }

        public long getNotHandledMessageCount() {
            long notHandled = notHandledMsgCnt.get();
            return notHandled < 0 ? 0 : notHandled;
        }
    }
}

