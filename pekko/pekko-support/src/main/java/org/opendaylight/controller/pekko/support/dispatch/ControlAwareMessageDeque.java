/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.dispatch;

import com.google.common.annotations.Beta;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.ControlMessage;
import org.apache.pekko.dispatch.DequeBasedMessageQueueSemantics;
import org.apache.pekko.dispatch.Envelope;
import org.apache.pekko.dispatch.UnboundedControlAwareMailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link UnboundedControlAwareMailbox.MessageQueue} also implementing {@link DequeBasedMessageQueueSemantics} via
 * two independent queues.
 *
 * @since 14.0.0
 */
@Beta
public final class ControlAwareMessageDeque extends UnboundedControlAwareMailbox.MessageQueue
        implements DequeBasedMessageQueueSemantics {
    private static final Logger LOG = LoggerFactory.getLogger(ControlAwareMessageDeque.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final ConcurrentLinkedDeque<Envelope> controlQueue;
    private final ConcurrentLinkedDeque<Envelope> queue;

    public ControlAwareMessageDeque() {
        controlQueue = new ConcurrentLinkedDeque<>();
        queue = new ConcurrentLinkedDeque<>();
    }

    public ControlAwareMessageDeque(final Collection<Envelope> control, final Collection<Envelope> normal) {
        controlQueue = new ConcurrentLinkedDeque<>(control);
        queue = new ConcurrentLinkedDeque<>(normal);
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
    public void enqueueFirst(final ActorRef actor, final Envelope envelope) {
        final var message = envelope.message();
        final var messageClass = message.getClass();
        LOG.trace("enqueueFirst: actor {}, message type: {}", actor, messageClass);
        final var tmp = switch (message) {
            case ControlMessage control -> {
                LOG.trace("Adding {} to the ControlMessage queue", messageClass);
                yield controlQueue;
            }
            default -> {
                LOG.trace("Adding {} to the normal queue", messageClass);
                yield queue;
            }
        };
        tmp.addFirst(envelope);
    }

    @java.io.Serial
    private Object writeReplace() {
        return new CAMDv1(controlQueue, queue);
    }
}
