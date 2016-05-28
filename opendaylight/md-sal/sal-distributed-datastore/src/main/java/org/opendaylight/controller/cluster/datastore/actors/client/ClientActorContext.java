/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An actor context associated with this {@link AbstractClientActor}.
 *
 * Time-keeping in a client actor is based on monotonic time. The precision of this time can be expected to be the
 * same as {@link System#nanoTime()}, but it is not tied to that particular clock. Actor clock is exposed as
 * a {@link Ticker}, which can be obtained via {@link #ticker()}.
 *
 * @author Robert Varga
 */
@Beta
@ThreadSafe
public class ClientActorContext extends AbstractClientActorContext implements Identifiable<ClientIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorContext.class);

    private static final class RequestEntry<I extends WritableIdentifier> {
        final Request<I, ?> request;
        final RequestCallback<I> callback;
        final long started;

        RequestEntry(final Request<I, ?> request, final RequestCallback<I> callback, final Ticker ticker) {
            this.request = Preconditions.checkNotNull(request);
            this.callback = Preconditions.checkNotNull(callback);
            started = ticker.read();
        }
    }

    private final Map<Identifier, RequestEntry<?>> requests = new ConcurrentHashMap<>();
    private final ClientIdentifier identifier;

    // Hidden to avoid subclassing
    ClientActorContext(final ActorRef self, final String persistenceId, final ClientIdentifier identifier) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public @Nonnull ClientIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Return the time ticker for this {@link ClientActorContext}. This should be used for in all time-tracking
     * down within a client actor. Subclasses of {@link ClientActorBehavior} are encouraged to use
     * {@link com.google.common.base.Stopwatch}.
     *
     * @return Client actor time source
     */
    public @Nonnull Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * Execute a command in the context of the client actor.
     *
     * @param command Block of code which needs to be execute
     */
    public void executeInActor(final @Nonnull InternalCommand command) {
        self().tell(Preconditions.checkNotNull(command), ActorRef.noSender());
    }

    <I extends WritableIdentifier, T extends Request<I, T>> void addRequest(final T request,
            final RequestCallback<I> onResponse) {
        requests.put(request.getTarget(), new RequestEntry<>(request, onResponse, ticker()));
    }

    long timeoutRequests(final long delta, final Consumer<Entry<Request<?, ?>, RequestCallback<?>>> callback) {
        Preconditions.checkArgument(delta > 0);
        final long now = ticker().read();
        long next = Long.MAX_VALUE;

        final Iterator<RequestEntry<?>> it = requests.values().iterator();
        while (it.hasNext()) {
            final RequestEntry<?> e = it.next();
            final long elapsed = now - e.started;
            final long diff = delta - elapsed;
            if (diff <= 0) {
                LOG.debug("Request {} timed out", e.request);
                callback.accept(new SimpleImmutableEntry<>(e.request, e.callback));
                it.remove();
            } else {
                if (next > diff) {
                    next = diff;
                }
            }
        }

        // next deadline
        LOG.trace("Next deadline in {}ns", next);
        return now + next;
    }
}
