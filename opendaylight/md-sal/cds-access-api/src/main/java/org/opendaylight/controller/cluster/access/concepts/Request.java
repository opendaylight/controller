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
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * A request message concept. Upon receipt of this message, the recipient will respond with either
 * a {@link RequestSuccess} or a {@link RequestFailure} message.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class Request<T extends Identifier & WritableObject> extends Message<T, Request<T>> {
    private static final long serialVersionUID = 1L;
    private final ActorRef replyTo;

    protected Request(final T target, final long sequence, final ActorRef replyTo) {
        super(target, sequence);
        this.replyTo = Preconditions.checkNotNull(replyTo);
    }

    public final ActorRef getReplyTo() {
        return replyTo;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("replyTo", replyTo);
    }

    @Override
    protected abstract AbstractRequestProxy<T> externalizableProxy();
}
