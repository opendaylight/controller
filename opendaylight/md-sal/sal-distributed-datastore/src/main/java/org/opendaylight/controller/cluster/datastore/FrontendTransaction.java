/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Frontend common transaction state as observed by the shard leader.
 *
 * @author Robert Varga
 */
@NotThreadSafe
abstract class FrontendTransaction implements Identifiable<TransactionIdentifier> {
    private final AbstractFrontendHistory history;
    private final TransactionIdentifier id;

    /**
     * It is possible that after we process a request and send a response that response gets lost and the client
     * initiates a retry. Since subsequent requests can mutate transaction state we need to retain the response until
     * it is acknowledged by the client.
     */
    private final Queue<Object> replayQueue = new ArrayDeque<>();
    private long firstReplaySequence;
    private Long lastPurgedSequence;
    private long expectedSequence;

    FrontendTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id) {
        this.history = Preconditions.checkNotNull(history);
        this.id = Preconditions.checkNotNull(id);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return id;
    }

    final AbstractFrontendHistory history() {
        return history;
    }

    final java.util.Optional<TransactionSuccess<?>> replaySequence(final long sequence) throws RequestException {
        // Fast path check: if the requested sequence is the next request, bail early
        if (expectedSequence == sequence) {
            return java.util.Optional.empty();
        }

        // Check sequencing: we do not need to bother with future requests
        if (Long.compareUnsigned(expectedSequence, sequence) < 0) {
            throw new OutOfOrderRequestException(expectedSequence);
        }

        // Sanity check: if we have purged sequences, this has to be newer
        if (lastPurgedSequence != null && Long.compareUnsigned(lastPurgedSequence.longValue(), sequence) >= 0) {
            // Client has sent a request sequence, which has already been purged. This is a hard error, which should
            // never occur. Throwing an IllegalArgumentException will cause it to be wrapped in a
            // RuntimeRequestException (which is not retriable) and report it back to the client.
            throw new IllegalArgumentException(String.format("Invalid purged sequence %s (last purged is %s)",
                sequence, lastPurgedSequence));
        }

        // At this point we have established that the requested sequence lies in the open interval
        // (lastPurgedSequence, expectedSequence). That does not actually mean we have a response, as the commit
        // machinery is asynchronous, hence a reply may be in the works and not available.

        long replaySequence = firstReplaySequence;
        final Iterator<?> it = replayQueue.iterator();
        while (it.hasNext()) {
            final Object replay = it.next();
            if (replaySequence == sequence) {
                if (replay instanceof RequestException) {
                    throw (RequestException) replay;
                }

                Verify.verify(replay instanceof TransactionSuccess);
                return java.util.Optional.of((TransactionSuccess<?>) replay);
            }

            replaySequence++;
        }

        // Not found
        return java.util.Optional.empty();
    }

    final void purgeSequencesUpTo(final long sequence) {
        // FIXME: implement this

        lastPurgedSequence = sequence;
    }

    // Sequence has already been checked
    abstract @Nullable TransactionSuccess<?> handleRequest(final TransactionRequest<?> request,
            final RequestEnvelope envelope, final long now) throws RequestException;

    private void recordResponse(final long sequence, final Object response) {
        if (replayQueue.isEmpty()) {
            firstReplaySequence = sequence;
        }
        replayQueue.add(response);
        expectedSequence++;
    }

    final <T extends TransactionSuccess<?>> T recordSuccess(final long sequence, final T success) {
        recordResponse(sequence, success);
        return success;
    }

    private long executionTime(final long startTime) {
        return history.readTime() - startTime;
    }

    final void recordAndSendSuccess(final RequestEnvelope envelope, final long startTime,
            final TransactionSuccess<?> success) {
        recordResponse(success.getSequence(), success);
        envelope.sendSuccess(success, executionTime(startTime));
    }

    final void recordAndSendFailure(final RequestEnvelope envelope, final long startTime,
            final RuntimeRequestException failure) {
        recordResponse(envelope.getMessage().getSequence(), failure);
        envelope.sendFailure(failure, executionTime(startTime));
    }
}
