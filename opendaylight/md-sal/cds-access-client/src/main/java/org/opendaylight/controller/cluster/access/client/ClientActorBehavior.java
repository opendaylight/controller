/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * A behavior, which handles messages sent to a {@link AbstractClientActor}.
 *
 * @author Robert Varga
 */
@Beta
public abstract class ClientActorBehavior extends RecoveredClientActorBehavior<ClientActorContext>
        implements Identifiable<ClientIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorBehavior.class);

    protected ClientActorBehavior(final @Nonnull ClientActorContext context) {
        super(context);
    }

    @Override
    public final @Nonnull ClientIdentifier getIdentifier() {
        return context().getIdentifier();
    }

    @Override
    final ClientActorBehavior onReceiveCommand(final Object command) {
        if (command instanceof InternalCommand) {
            return ((InternalCommand) command).execute(this);
        }
        if (command instanceof SuccessEnvelope) {
            return onRequestSuccess((SuccessEnvelope) command);
        }
        if (command instanceof FailureEnvelope) {
            return onRequestFailure((FailureEnvelope) command);
        }

        return onCommand(command);
    }

    private ClientActorBehavior onRequestSuccess(final SuccessEnvelope command) {
        return context().completeRequest(this, command);
    }

    private ClientActorBehavior onRequestFailure(final FailureEnvelope command) {
        final RequestFailure<?, ?> failure = command.getMessage();
        final RequestException cause = failure.getCause();
        if (cause instanceof RetiredGenerationException) {
            LOG.error("{}: current generation {} has been superseded", persistenceId(), getIdentifier(), cause);
            haltClient(cause);
            context().poison(cause);
            return null;
        }

        if (failure.isHardFailure()) {
            return context().completeRequest(this, command);
        }

        // TODO: add instanceof checks on cause to detect more problems

        LOG.warn("{}: Unhandled retriable failure {}, promoting to hard failure", persistenceId(), command);
        return context().completeRequest(this, command);
    }

    // This method is executing in the actor context, hence we can safely interact with the queue
    private ClientActorBehavior doSendRequest(final TransactionRequest<?> request, final RequestCallback callback) {
        // Get or allocate queue for the request
        final SequencedQueue queue = context().queueFor(request.getTarget().getHistoryId().getCookie());

        // Note this is a tri-state return and can be null
        final Optional<FiniteDuration> result = queue.enqueueRequest(request, callback);
        if (result == null) {
            // Happy path: we are done here
            return this;
        }

        if (result.isPresent()) {
            // Less happy path: we need to schedule a timer
            scheduleQueueTimeout(queue, result.get());
            return this;
        }

        startResolve(queue, request.getTarget().getHistoryId().getCookie());
        return this;
    }

    // This method is executing in the actor context, hence we can safely interact with the queue
    private void startResolve(final SequencedQueue queue, final long cookie) {
        // Queue does not have backend information. Initiate resolution, which may actually be piggy-backing on to a
        // previous request to resolve.
        final CompletionStage<? extends BackendInfo> f = resolver().getBackendInfo(cookie);

        // This is the tricky part: depending on timing, the queue may have a stale request for resolution, which has
        // been invalidated or it may already have a reference to this resolution request. Let us give it a chance to
        // update and it will indicate if this resolution request is an update. If it is, we'll piggy-back on it and
        // run backend information update in the actor thread. If it is not, we do not need to do anything, as we will
        // bulk-process all requests.
        if (queue.expectProof(f)) {
            f.thenAccept(backend -> context().executeInActor(cb -> cb.finishResolve(queue, f, backend)));
        }
    }

    // This method is executing in the actor context, hence we can safely interact with the queue
    private ClientActorBehavior finishResolve(final SequencedQueue queue,
            final CompletionStage<? extends BackendInfo> futureBackend, final BackendInfo backend) {

        final Optional<FiniteDuration> maybeTimeout = queue.setBackendInfo(futureBackend, backend);
        if (maybeTimeout.isPresent()) {
            scheduleQueueTimeout(queue, maybeTimeout.get());
        }
        return this;
    }

    // This method is executing in the actor context, hence we can safely interact with the queue
    private void scheduleQueueTimeout(final SequencedQueue queue, final FiniteDuration timeout) {
        LOG.debug("{}: scheduling timeout in {}", persistenceId(), timeout);
        context().executeInActor(cb -> cb.queueTimeout(queue), timeout);
    }

    // This method is executing in the actor context, hence we can safely interact with the queue
    private ClientActorBehavior queueTimeout(final SequencedQueue queue) {
        final boolean needBackend;

        try {
            needBackend = queue.runTimeout();
        } catch (NoProgressException e) {
            // Uh-oh, no progress. The queue has already killed itself, now we need to remove it
            context().removeQueue(queue);
            return this;
        }

        if (needBackend) {
            startResolve(queue, queue.getCookie());
        }

        return this;
    }

    /**
     * Halt And Catch Fire.
     *
     * Halt processing on this client. Implementations need to ensure they initiate state flush procedures. No attempt
     * to use this instance should be made after this method returns. Any such use may result in undefined behavior.
     *
     * @param cause Failure cause
     */
    protected abstract void haltClient(@Nonnull Throwable cause);

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command
     * @return Next behavior to use, null if this actor should shut down.
     */
    protected abstract @Nullable ClientActorBehavior onCommand(@Nonnull Object command);

    /**
     * Override this method to provide a backend resolver instance.
     *
     * @return
     */
    protected abstract @Nonnull BackendInfoResolver<?> resolver();

    /**
     * Send a request to the backend and invoke a specified callback when it finishes. This method is safe to invoke
     * from any thread.
     *
     * @param request Request to send
     * @param callback Callback to invoke
     */
    public final void sendRequest(final TransactionRequest<?> request, final RequestCallback callback) {
        context().executeInActor(cb -> cb.doSendRequest(request, callback));
    }
}
