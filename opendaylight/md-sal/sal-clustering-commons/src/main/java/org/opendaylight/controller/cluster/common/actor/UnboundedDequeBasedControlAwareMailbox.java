/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import com.codahale.metrics.Gauge;
import com.typesafe.config.Config;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.dispatch.DequeBasedMessageQueueSemantics;
import org.apache.pekko.dispatch.Envelope;
import org.apache.pekko.dispatch.MailboxType;
import org.apache.pekko.dispatch.ProducesMessageQueue;
import org.apache.pekko.dispatch.UnboundedControlAwareMailbox;
import org.opendaylight.controller.pekko.support.dispatch.ControlAwareMessageDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

/**
 * An unbounded ControlAwareMailbox that also supports {@link DequeBasedMessageQueueSemantics} so it can be used with
 * persistent actors which use stashing.
 *
 * @author Thomas Pantelis
 */
public class UnboundedDequeBasedControlAwareMailbox
        implements MailboxType, ProducesMessageQueue<ControlAwareMessageDeque> {
    private static final Logger LOG = LoggerFactory.getLogger(UnboundedDequeBasedControlAwareMailbox.class);
    private static final String NORMAL_QUEUE_SIZE = "normal-q-size";
    private static final String CONTROL_QUEUE_SIZE = "control-q-size";
    private static final String TOTAL_QUEUE_SIZE = "total-q-size";

    public UnboundedDequeBasedControlAwareMailbox(final ActorSystem.Settings settings, final Config config) {
        // nothing here
    }

    @Override
    public ControlAwareMessageDeque create(final Option<ActorRef> owner,
            final Option<ActorSystem> system) {
        LOG.debug("Creating MessageQueue for {}", owner);

        final var queue = new ControlAwareMessageDeque();

        MeteredBoundedMailbox.registerMetric(owner, NORMAL_QUEUE_SIZE, (Gauge<Integer>) () -> queue.queue().size());
        MeteredBoundedMailbox.registerMetric(owner, CONTROL_QUEUE_SIZE,
            (Gauge<Integer>) () -> queue.controlQueue().size());
        MeteredBoundedMailbox.registerMetric(owner, TOTAL_QUEUE_SIZE, (Gauge<Integer>) queue::numberOfMessages);

        return queue;
    }

    @Deprecated(since = "14.0.0", forRemoval = true)
    static class MessageQueue extends UnboundedControlAwareMailbox.MessageQueue
            implements DequeBasedMessageQueueSemantics {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private final Deque<Envelope> controlQueue = new ConcurrentLinkedDeque<>();
        private final Deque<Envelope> queue = new ConcurrentLinkedDeque<>();

        private MessageQueue() {
            // hidden on purpose
        }

        @Override
        @Deprecated(since = "14.0.0", forRemoval = true)
        public Queue<Envelope> controlQueue() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated(since = "14.0.0", forRemoval = true)
        public Queue<Envelope> queue() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated(since = "14.0.0", forRemoval = true)
        public void enqueueFirst(final ActorRef actor, final Envelope envelope) {
            throw new UnsupportedOperationException();
        }

        @java.io.Serial
        private Object readResolve() {
            return new ControlAwareMessageDeque(controlQueue, queue);
        }
    }
}
