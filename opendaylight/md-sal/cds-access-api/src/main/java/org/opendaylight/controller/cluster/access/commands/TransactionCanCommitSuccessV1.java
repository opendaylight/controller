/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link TransactionCanCommitSuccess}. It implements the initial (Boron)
 * serialization format.
 *
 * @author Robert Varga
 */
final class TransactionCanCommitSuccessV1 extends AbstractTransactionSuccessProxy<TransactionCanCommitSuccess> {
    private static final long serialVersionUID = 1L;
    private ActorRef cohort;

    public TransactionCanCommitSuccessV1() {
        // For Externalizable
    }

    TransactionCanCommitSuccessV1(final TransactionCanCommitSuccess success) {
        super(success);
        this.cohort = success.getCohort();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(Serialization.serializedActorPath(cohort));
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        cohort = JavaSerializer.currentSystem().value().provider().resolveActorRef(in.readUTF());
    }

    @Override
    protected TransactionCanCommitSuccess createSuccess(final TransactionIdentifier target, final long sequence,
            final long retry) {
        return new TransactionCanCommitSuccess(target, sequence, retry, cohort);
    }
}
