/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * A reusable builder for creating {@link ModifyTransactionRequest} message instances. Its internal state is reset when
 * {@link #build()} is invoked, hence it can be used to create a sequence of messages. This class is NOT thread-safe.
 */
public final class ModifyTransactionRequestBuilder implements Identifiable<TransactionIdentifier> {
    private final List<TransactionModification> modifications = new ArrayList<>(1);
    private final @NonNull TransactionIdentifier identifier;
    private final ActorRef replyTo;

    private PersistenceProtocol protocol;
    private boolean haveSequence;
    private long sequence;

    public ModifyTransactionRequestBuilder(final TransactionIdentifier identifier, final ActorRef replyTo) {
        this.identifier = requireNonNull(identifier);
        this.replyTo = requireNonNull(replyTo);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return identifier;
    }

    private void checkNotFinished() {
        checkState(protocol == null, "Batch has already been finished");
    }

    public void addModification(final TransactionModification modification) {
        checkNotFinished();
        modifications.add(requireNonNull(modification));
    }

    public void setSequence(final long sequence) {
        checkState(!haveSequence, "Sequence has already been set");
        this.sequence = sequence;
        haveSequence = true;
    }

    public void setAbort() {
        checkNotFinished();
        // Transaction is being aborted, no need to transmit operations
        modifications.clear();
        protocol = PersistenceProtocol.ABORT;
    }

    public void setCommit(final boolean coordinated) {
        checkNotFinished();
        protocol = coordinated ? PersistenceProtocol.THREE_PHASE : PersistenceProtocol.SIMPLE;
    }

    public void setReady() {
        checkNotFinished();
        protocol = PersistenceProtocol.READY;
    }

    public int size() {
        return modifications.size();
    }

    public @NonNull ModifyTransactionRequest build() {
        checkState(haveSequence, "Request sequence has not been set");

        final ModifyTransactionRequest ret = new ModifyTransactionRequest(identifier, sequence, replyTo, modifications,
            protocol);
        modifications.clear();
        protocol = null;
        haveSequence = false;
        return ret;
    }
}
