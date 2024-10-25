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

import java.util.ArrayList;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A reusable builder for creating {@link ModifyTransactionRequest} message instances. Its internal state is reset when
 * {@link #build()} is invoked, hence it can be used to create a sequence of messages. This class is NOT thread-safe.
 */
public final class ModifyTransactionRequestBuilder implements Identifiable<TransactionIdentifier> {
    private final ArrayList<TransactionModification> modifications = new ArrayList<>(1);
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

    public ModifyTransactionRequestBuilder addModification(final TransactionModification modification) {
        checkNotFinished();
        modifications.add(requireNonNull(modification));
        return this;
    }

    public ModifyTransactionRequestBuilder addDelete(final YangInstanceIdentifier path) {
        return addModification(new TransactionDelete(path));
    }

    public ModifyTransactionRequestBuilder addMerge(final YangInstanceIdentifier path, final NormalizedNode data) {
        return addModification(new TransactionMerge(path, data));
    }

    public ModifyTransactionRequestBuilder addWrite(final YangInstanceIdentifier path, final NormalizedNode data) {
        return addModification(new TransactionWrite(path, data));
    }

    public ModifyTransactionRequestBuilder setSequence(final long sequence) {
        checkState(!haveSequence, "Sequence has already been set");
        this.sequence = sequence;
        haveSequence = true;
        return this;
    }

    public ModifyTransactionRequestBuilder setAbort() {
        checkNotFinished();
        // Transaction is being aborted, no need to transmit operations
        modifications.clear();
        protocol = PersistenceProtocol.ABORT;
        return this;
    }

    public ModifyTransactionRequestBuilder setCommit(final boolean coordinated) {
        checkNotFinished();
        protocol = coordinated ? PersistenceProtocol.THREE_PHASE : PersistenceProtocol.SIMPLE;
        return this;
    }

    public ModifyTransactionRequestBuilder setReady() {
        checkNotFinished();
        protocol = PersistenceProtocol.READY;
        return this;
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
