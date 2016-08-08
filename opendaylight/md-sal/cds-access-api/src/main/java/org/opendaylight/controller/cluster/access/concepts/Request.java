/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A request message concept. Upon receipt of this message, the recipient will respond with either
 * a {@link RequestSuccess} or a {@link RequestFailure} message.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message type
 */
@Beta
public abstract class Request<T extends WritableIdentifier, C extends Request<T, C>> extends Message<T, C> {
    private static final long serialVersionUID = 1L;
    private final ActorRef replyTo;

    protected Request(final @Nonnull T target, final long sequence, final @Nonnull ActorRef replyTo) {
        super(target, sequence);
        this.replyTo = Preconditions.checkNotNull(replyTo);
    }

    protected Request(final @Nonnull C request, final @Nonnull ABIVersion version) {
        super(request, version);
        this.replyTo = Preconditions.checkNotNull(request.getReplyTo());
    }

    /**
     * Return the return address where responses to this request should be directed to.
     *
     * @return Original requestor
     */
    public final @Nonnull ActorRef getReplyTo() {
        return replyTo;
    }

    /**
     * Return a {@link RequestFailure} for this request, caused by a {@link RequestException}.
     *
     * @param cause Failure cause
     * @return {@link RequestFailure} corresponding to this request
     */
    public abstract @Nonnull RequestFailure<T, ?> toRequestFailure(final @Nonnull RequestException cause);

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("replyTo", replyTo);
    }

    @Override
    protected abstract AbstractRequestProxy<T, C> externalizableProxy(@Nonnull ABIVersion version);
}
