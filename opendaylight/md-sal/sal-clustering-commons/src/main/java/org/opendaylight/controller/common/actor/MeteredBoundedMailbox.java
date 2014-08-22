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
import akka.dispatch.BoundedDequeBasedMailbox;
import akka.dispatch.MailboxType;
import akka.dispatch.ProducesMessageQueue;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import org.opendaylight.controller.common.reporting.MetricsReporter;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class MeteredBoundedMailbox implements MailboxType, ProducesMessageQueue<MeteredBoundedMailbox.MeteredMessageQueue> {

    private MeteredMessageQueue queue;
    private Integer capacity;
    private FiniteDuration pushTimeOut;
    private ActorPath actorPath;
    private MetricsReporter reporter;

    private final String QUEUE_SIZE = "queue-size";
    private final String CAPACITY = "mailbox-capacity";
    private final String TIMEOUT  = "mailbox-push-timeout-time";
    private final Long DEFAULT_TIMEOUT = 10L;

    public MeteredBoundedMailbox(ActorSystem.Settings settings, Config config) {
        Preconditions.checkArgument( config.hasPath("mailbox-capacity"), "Missing configuration [mailbox-capacity]" );
        this.capacity = config.getInt(CAPACITY);
        Preconditions.checkArgument( this.capacity > 0, "mailbox-capacity must be > 0");

        Long timeout = -1L;
        if ( config.hasPath(TIMEOUT) ){
            timeout = config.getDuration(TIMEOUT, TimeUnit.NANOSECONDS);
        } else {
            timeout = DEFAULT_TIMEOUT;
        }
        Preconditions.checkArgument( timeout > 0, "mailbox-push-timeout-time must be > 0");
        this.pushTimeOut = new FiniteDuration(timeout, TimeUnit.NANOSECONDS);

        reporter = MetricsReporter.getInstance();
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
        actorPath = owner.get().path();
        String actorInstanceId = Integer.toString(owner.get().hashCode());

        MetricRegistry registry = reporter.getMetricsRegistry();
        String actorName = registry.name(actorPath.toString(), actorInstanceId, QUEUE_SIZE);

        if (registry.getMetrics().containsKey(actorName))
            return; //already registered

        registry.register(actorName,
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return monitoredQueue.size();
                    }
                });
    }


    public static class MeteredMessageQueue extends BoundedDequeBasedMailbox.MessageQueue {

        public MeteredMessageQueue(int capacity, FiniteDuration pushTimeOut) {
            super(capacity, pushTimeOut);
        }
    }

}

