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
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

@Beta
public final class SkipTransactionsLocalHistoryRequest
        extends LocalHistoryRequest<SkipTransactionsLocalHistoryRequest> {
    private static final long serialVersionUID = 1L;

    private final @NonNull ImmutableList<UnsignedLong> transactionIds;

    public SkipTransactionsLocalHistoryRequest(final LocalHistoryIdentifier target, final long sequence,
            final ActorRef replyTo, final List<UnsignedLong> transactionIds) {
        super(target, sequence, replyTo);
        this.transactionIds = ImmutableList.copyOf(transactionIds);
    }

    public List<UnsignedLong> getTransactionIds() {
        return transactionIds;
    }

    @Override
    protected SkipTransactionsLocalHistoryRequestV1 externalizableProxy(final ABIVersion version) {
        return new SkipTransactionsLocalHistoryRequestV1(this);
    }

    @Override
    protected SkipTransactionsLocalHistoryRequest cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
