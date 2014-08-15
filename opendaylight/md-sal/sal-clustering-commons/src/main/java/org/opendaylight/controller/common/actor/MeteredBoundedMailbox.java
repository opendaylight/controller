/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.common.actor;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.BoundedMailbox;
import akka.dispatch.MailboxType;
import akka.dispatch.MessageQueue;
import akka.dispatch.ProducesMessageQueue;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.Config;
import org.opendaylight.controller.common.reporting.MetricsReporter;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class MeteredBoundedMailbox implements MailboxType, ProducesMessageQueue<BoundedMailbox.MessageQueue> {

    private MeteredMessageQueue queue;
    private int capacity;
    private FiniteDuration pushTimeOut;
    private ActorPath actorPath;
    private final String QUEUE_SIZE = "queue-size";
    private MetricsReporter reporter;

    public MeteredBoundedMailbox(ActorSystem.Settings settings, Config config) {
        this.capacity = config.getInt("mailbox-capacity");
        long timeout = config.getDuration("mailbox-push-timeout-time", TimeUnit.NANOSECONDS);
        this.pushTimeOut = new FiniteDuration(timeout, TimeUnit.NANOSECONDS);

        reporter = MetricsReporter.getInstance();
    }


    @Override
    public MessageQueue create(scala.Option<ActorRef> owner, scala.Option<ActorSystem> system) {
        this.queue = new MeteredMessageQueue(this.capacity, this.pushTimeOut);
        monitorQueueSize(owner, this.queue);
        return this.queue;
    }

    private void monitorQueueSize(scala.Option<ActorRef> owner, final MeteredMessageQueue monitoredQueue) {
        if (owner.isEmpty()) {
            return; //there's no actor to monitor
        }
        actorPath = owner.get().path();
        MetricRegistry registry = reporter.getMetricsRegistry();

        String actorName = registry.name(actorPath.toString(), QUEUE_SIZE);

        if (registry.getMetrics().containsKey(actorName))
            return; //already registered

        reporter.getMetricsRegistry().register(actorName,
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return monitoredQueue.size();
                    }
                });
    }


    public static class MeteredMessageQueue extends BoundedMailbox.MessageQueue {

        public MeteredMessageQueue(int capacity, FiniteDuration pushTimeOut) {
            super(capacity, pushTimeOut);
        }
    }

}

