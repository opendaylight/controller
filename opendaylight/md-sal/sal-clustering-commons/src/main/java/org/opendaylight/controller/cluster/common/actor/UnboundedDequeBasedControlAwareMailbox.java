/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
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
 * An unbounded ControlAwareMailbox that also supports DequeBasedMessageQueueSemantics so it can be used with
 * persistent actors which use stashing.
 *
 * @author Thomas Pantelis
 */
public class UnboundedDequeBasedControlAwareMailbox implements MailboxType,
        ProducesMessageQueue<UnboundedDequeBasedControlAwareMailbox.MessageQueue> {
    private static final Logger LOG = LoggerFactory.getLogger(UnboundedDequeBasedControlAwareMailbox.class);
    private static final String NORMAL_QUEUE_SIZE = "normal-q-size";
    private static final String CONTROL_QUEUE_SIZE = "control-q-size";
    private static final String TOTAL_QUEUE_SIZE = "total-q-size";

    public UnboundedDequeBasedControlAwareMailbox(ActorSystem.Settings settings, Config config) {
    }

    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        LOG.debug("Creating MessageQueue for {}", owner);

        final MessageQueue queue = new MessageQueue();

        MeteredBoundedMailbox.registerMetric(owner, NORMAL_QUEUE_SIZE, (Gauge<Integer>) () -> queue.queue().size());
        MeteredBoundedMailbox.registerMetric(owner, CONTROL_QUEUE_SIZE,
            (Gauge<Integer>) () -> queue.controlQueue().size());
        MeteredBoundedMailbox.registerMetric(owner, TOTAL_QUEUE_SIZE, (Gauge<Integer>) queue::numberOfMessages);

        return queue;
    }

    static class MessageQueue extends UnboundedControlAwareMailbox.MessageQueue
            implements DequeBasedMessageQueueSemantics {
        private static final long serialVersionUID = 1L;

        private final Deque<Envelope> controlQueue = new ConcurrentLinkedDeque<>();
        private final Deque<Envelope> queue = new ConcurrentLinkedDeque<>();

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
                LOG.trace("Adding {} to the ControlMessage queue", message.getClass());
                controlQueue.addFirst(envelope);
            } else {
                LOG.trace("Adding {} to the normal queue", message.getClass());
                queue.addFirst(envelope);
            }
        }
    }
}
