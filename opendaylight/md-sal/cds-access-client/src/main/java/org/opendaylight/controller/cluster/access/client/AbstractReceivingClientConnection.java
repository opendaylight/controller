/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Implementation-internal intermediate subclass between {@link AbstractClientConnection} and two-out of three of its
 * sublcasses. It allows us to share some code.
 *
 * @author Robert Varga
 *
 * @param <T> Concrete {@link BackendInfo} type
 */
abstract class AbstractReceivingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReceivingClientConnection.class);

    private final Queue<TransmittedConnectionEntry> inflight = new ArrayDeque<>();
    private final T backend;

    private long lastProgress;

    AbstractReceivingClientConnection(final ClientActorContext context, final Long cookie, final T backend) {
        super(context, cookie);
        this.backend = Preconditions.checkNotNull(backend);
        this.lastProgress = readTime();
    }

    AbstractReceivingClientConnection(final AbstractReceivingClientConnection<T> oldConnection) {
        super(oldConnection);
        this.backend = oldConnection.backend;
        this.lastProgress = oldConnection.lastProgress;
    }

    @Override
    public final Optional<T> getBackendInfo() {
        return Optional.of(backend);
    }

    final ActorRef remoteActor() {
        return backend.getActor();
    }

    final int remoteMaxMessages() {
        return backend.getMaxMessages();
    }

    final ABIVersion remoteVersion() {
        return backend.getVersion();
    }

    final long sessionId() {
        return backend.getSessionId();
    }

    final int inflightSize() {
        return inflight.size();
    }

    final void appendToInflight(final TransmittedConnectionEntry entry) {
        // This should never fail
        inflight.add(entry);
    }

    @GuardedBy("this")
    @Override
    void spliceToSuccessor(final ReconnectForwarder successor) {
        ConnectionEntry entry = inflight.poll();
        while (entry != null) {
            successor.forwardEntry(entry);
            entry = inflight.poll();
        }

        super.spliceToSuccessor(successor);
    }

    @Override
    void receiveResponse(final ResponseEnvelope<?> envelope) {
        Optional<TransmittedConnectionEntry> maybeEntry = findMatchingEntry(inflight, envelope);
        if (maybeEntry == null) {
            LOG.debug("Request for {} not found in inflight queue, checking pending queue", envelope);
            maybeEntry = findMatchingEntry(pending(), envelope);
        }

        if (maybeEntry == null || !maybeEntry.isPresent()) {
            LOG.warn("No request matching {} found, ignoring response", envelope);
            return;
        }

        lastProgress = readTime();

        final TransmittedConnectionEntry entry = maybeEntry.get();
        LOG.debug("Completing {} with {}", entry, envelope);
        entry.complete(envelope.getMessage());

        // We have freed up a slot, try to transmit something
        final int toSend = remoteMaxMessages() - inflight.size();
        if (toSend > 0) {
            sendMessages(toSend);
        }
    }

    @Override
    boolean isEmpty() {
        return inflight.isEmpty() && super.isEmpty();
    }

    @Override
    void poison(final RequestException cause) {
        super.poison(cause);
        poisonQueue(inflight, cause);
    }

    /**
     * Transmit a given number of messages.
     *
     * @param count Number of messages to transmit, guaranteed to be positive.
     */
    abstract void sendMessages(int count);

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if a matching entry is found, return an Optional containing it
     * - if a matching entry is not found, but it makes sense to keep looking at other queues, return null
     * - if a conflicting entry is encountered, indicating we should ignore this request, return an empty Optional
     */
    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    private static Optional<TransmittedConnectionEntry> findMatchingEntry(final Queue<? extends ConnectionEntry> queue,
            final ResponseEnvelope<?> envelope) {
        // Try to find the request in a queue. Responses may legally come back in a different order, hence we need
        // to use an iterator
        final Iterator<? extends ConnectionEntry> it = queue.iterator();
        while (it.hasNext()) {
            final ConnectionEntry e = it.next();
            final Request<?, ?> request = e.getRequest();
            final Response<?, ?> response = envelope.getMessage();

            // First check for matching target, or move to next entry
            if (!request.getTarget().equals(response.getTarget())) {
                continue;
            }

            // Sanity-check logical sequence, ignore any out-of-order messages
            if (request.getSequence() != response.getSequence()) {
                LOG.debug("Expecting sequence {}, ignoring response {}", request.getSequence(), envelope);
                return Optional.empty();
            }

            // Check if the entry has (ever) been transmitted
            if (!(e instanceof TransmittedConnectionEntry)) {
                return Optional.empty();
            }

            final TransmittedConnectionEntry te = (TransmittedConnectionEntry) e;

            // Now check session match
            if (envelope.getSessionId() != te.getSessionId()) {
                LOG.debug("Expecting session {}, ignoring response {}", te.getSessionId(), envelope);
                return Optional.empty();
            }
            if (envelope.getTxSequence() != te.getTxSequence()) {
                LOG.warn("Expecting txSequence {}, ignoring response {}", te.getTxSequence(), envelope);
                return Optional.empty();
            }

            LOG.debug("Completing request {} with {}", request, envelope);
            it.remove();
            return Optional.of(te);
        }

        return null;
    }

    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    @Override
    final Optional<FiniteDuration> checkTimeout(final long now) {
        final Optional<FiniteDuration> xmit = checkTimeout(inflight.peek(), now);
        if (xmit == null) {
            return null;
        }
        final Optional<FiniteDuration> pend = super.checkTimeout(now);
        if (pend == null) {
            return null;
        }
        if (!xmit.isPresent()) {
            return pend;
        }
        if (!pend.isPresent()) {
            return xmit;
        }

        return Optional.of(xmit.get().min(pend.get()));
    }
}
