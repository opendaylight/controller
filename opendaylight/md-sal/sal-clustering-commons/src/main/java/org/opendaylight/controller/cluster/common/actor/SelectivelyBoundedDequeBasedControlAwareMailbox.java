/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.ControlMessage;
import akka.dispatch.DequeBasedMessageQueueSemantics;
import akka.dispatch.Envelope;
import akka.dispatch.MailboxType;
import akka.dispatch.ProducesMessageQueue;
import akka.dispatch.UnboundedControlAwareMailbox;
import com.codahale.metrics.Gauge;
import com.typesafe.config.Config;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

/**
 * By default an unbounded ControlAwareMailbox that also supports DequeBasedMessageQueueSemantics so it can be used with
 * persistent actors which use stashing. This implementation can however put a limit on the size of queues, which
 * can prevent OOM in case the actor can't process messages quicker than they arrive.
 *
 */
public class SelectivelyBoundedDequeBasedControlAwareMailbox implements MailboxType,
        ProducesMessageQueue<SelectivelyBoundedDequeBasedControlAwareMailbox.MessageQueue> {
    private static final Logger LOG = LoggerFactory.getLogger(SelectivelyBoundedDequeBasedControlAwareMailbox.class);
    private static final String NORMAL_QUEUE_SIZE = "normal-q-size";
    private static final String CONTROL_QUEUE_SIZE = "control-q-size";
    private static final String TOTAL_QUEUE_SIZE = "total-q-size";
    private final Config config;

    public SelectivelyBoundedDequeBasedControlAwareMailbox(ActorSystem.Settings settings, Config config) {
        this.config = config;
    }

    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        LOG.debug("Creating MessageQueue for {}", owner);

        final MessageQueue queue = new MessageQueue(config);

        MeteredBoundedMailbox.registerMetric(owner, NORMAL_QUEUE_SIZE, (Gauge<Integer>) () -> queue.queue().size());
        MeteredBoundedMailbox.registerMetric(owner, CONTROL_QUEUE_SIZE,
            (Gauge<Integer>) () -> queue.controlQueue().size());
        MeteredBoundedMailbox.registerMetric(owner, TOTAL_QUEUE_SIZE, (Gauge<Integer>) queue::numberOfMessages);

        return queue;
    }

    static class MessageQueue extends UnboundedControlAwareMailbox.MessageQueue
            implements DequeBasedMessageQueueSemantics {
        private static final long serialVersionUID = 1L;
        private static final int NO_CAPACITY_LIMIT = 0;

        private final Deque<Envelope> controlQueue = new ConcurrentLinkedDeque<>();
        private final Deque<Envelope> queue = new ConcurrentLinkedDeque<>();

        private final int maxControlQueueCapacity;
        private final int maxNormalQueueCapacity;

        MessageQueue(final Config config) {
            if (config.hasPath("limit-control-queue-capacity")) {
                int capacity = config.getInt("limit-control-queue-capacity");
                this.maxControlQueueCapacity = capacity > 0 ? capacity : NO_CAPACITY_LIMIT;
            } else {
                this.maxControlQueueCapacity = NO_CAPACITY_LIMIT;
            }
            if (config.hasPath("limit-normal-queue-capacity")) {
                int capacity = config.getInt("limit-normal-queue-capacity");
                this.maxNormalQueueCapacity = capacity > 0 ? capacity : NO_CAPACITY_LIMIT;
            } else {
                this.maxNormalQueueCapacity = NO_CAPACITY_LIMIT;
            }
        }

        @Override
        public Queue<Envelope> controlQueue() {
            return controlQueue;
        }

        @Override
        public Queue<Envelope> queue() {
            return queue;
        }

        @Override
        public void enqueueFirst(ActorRef actor, Envelope envelope) {
            final Object message = envelope.message();
            LOG.trace("enqueueFirst: actor {}, message type: {}", actor, message.getClass());
            if (message instanceof ControlMessage) {
                if (maxControlQueueCapacity > NO_CAPACITY_LIMIT && controlQueue.size() < maxControlQueueCapacity) {
                    controlQueue.addFirst(envelope);
                } else {
                    LOG.warn("Control MessageQueue full, dropping message {}", message.getClass());
                }
            } else {
                if (maxNormalQueueCapacity > NO_CAPACITY_LIMIT && queue.size() < maxNormalQueueCapacity) {
                    queue.addFirst(envelope);
                } else {
                    LOG.warn("Normal MessageQueue full, dropping message {}", message.getClass());
                }
            }
        }
    }
}
