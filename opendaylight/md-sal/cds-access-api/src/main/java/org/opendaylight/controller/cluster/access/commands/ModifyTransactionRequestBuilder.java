/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * A reusable {@link Builder} for creating {@link ModifyTransactionRequest} message instances. Its internal state is
 * reset when {@link #build()} is invoked, hence it can be used to create a sequence of messages.
 *
 * @author Robert Varga
 */
@Beta
@NotThreadSafe
public final class ModifyTransactionRequestBuilder implements Builder<ModifyTransactionRequest>,
        Identifiable<TransactionIdentifier> {
    private final List<TransactionModification> modifications = new ArrayList<>(1);
    private final TransactionIdentifier identifier;
    private final ActorRef replyTo;
    private PersistenceProtocol protocol;
    private Long sequence;

    public ModifyTransactionRequestBuilder(final TransactionIdentifier identifier, final ActorRef replyTo) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.replyTo = Preconditions.checkNotNull(replyTo);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return identifier;
    }

    private void checkNotFinished() {
        Preconditions.checkState(protocol == null, "Batch has already been finished");
    }

    public void addModification(final TransactionModification modification) {
        checkNotFinished();
        modifications.add(Preconditions.checkNotNull(modification));
    }

    public void setSequence(final long sequence) {
        this.sequence = sequence;
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

    public int size() {
        return modifications.size();
    }

    @Override
    public ModifyTransactionRequest build() {
        Preconditions.checkState(sequence != null, "Request sequence has not been set");

        final ModifyTransactionRequest ret = new ModifyTransactionRequest(identifier, sequence, replyTo, modifications,
            protocol);
        modifications.clear();
        protocol = null;
        sequence = null;
        return ret;
    }
}
