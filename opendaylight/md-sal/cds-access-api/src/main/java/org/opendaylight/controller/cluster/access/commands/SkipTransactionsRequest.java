/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Request to skip a number of {@link TransactionIdentifier}s within a {code local history}. This request is essentially
 * equivalent to {@link TransactionPurgeRequest} for {@link #getTarget()}, but also carries additional sibling
 * {@link TransactionIdentifier}s in {@link #getOthers()}.
 *
 * <p>
 * This request is sent by the frontend to inform the backend that a set of {@link TransactionIdentifier}s are
 * explicitly retired and are guaranteed to never be used by the frontend.
 */
@Beta
public final class SkipTransactionsRequest extends TransactionRequest<SkipTransactionsRequest> {
    private static final long serialVersionUID = 1L;

    // Note: UnsignedLong is arbitrary, yang.common.Uint64 would work just as well, we really want an immutable
    //       List<long>, though.
    private final @NonNull ImmutableList<UnsignedLong> others;

    public SkipTransactionsRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo, final Collection<UnsignedLong> others) {
        super(target, sequence, replyTo);
        this.others = ImmutableList.copyOf(others);
    }

    /**
     * Return this {@link #getTarget()}s sibling {@link TransactionIdentifier}s.
     *
     * @return Siblings values of {@link TransactionIdentifier#getTransactionId()}
     */
    public List<UnsignedLong> getOthers() {
        return others;
    }

    @Override
    protected SkipTransactionsRequestV1 externalizableProxy(final ABIVersion version) {
        return new SkipTransactionsRequestV1(this);
    }

    @Override
    protected SkipTransactionsRequest cloneAsVersion(final ABIVersion version) {
        return this;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        final var helper = super.addToStringAttributes(toStringHelper);
        if (!others.isEmpty()) {
            helper.add("others", others);
        }
        return helper;
    }
}
