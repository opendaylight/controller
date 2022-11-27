/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract base class for {@link TransactionRequest}s accessing transaction state without modifying it.
 *
 * <p>
 * This class is visible outside of this package for the purpose of allowing common instanceof checks
 * and simplified codepaths.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
@Beta
public abstract class AbstractReadTransactionRequest<T extends AbstractReadTransactionRequest<T>>
        extends TransactionRequest<T> {
    interface SerialForm<T extends AbstractReadTransactionRequest<T>> extends TransactionRequest.SerialForm<T> {
        @Override
        default T readExternal(final ObjectInput in, final TransactionIdentifier target, final long sequence,
                final ActorRef replyTo) throws IOException {
            return readExternal(in, target, sequence, replyTo, in.readBoolean());
        }

        @NonNull T readExternal(@NonNull ObjectInput in, @NonNull TransactionIdentifier target, long sequence,
            @NonNull ActorRef replyTo, boolean snapshotOnly) throws IOException;

        @Override
        default void writeExternal(final ObjectOutput out, final T msg) throws IOException {
            TransactionRequest.SerialForm.super.writeExternal(out, msg);
            out.writeBoolean(msg.isSnapshotOnly());
        }
    }

    private static final long serialVersionUID = 1L;

    private final boolean snapshotOnly;

    AbstractReadTransactionRequest(final TransactionIdentifier identifier, final long sequence, final ActorRef replyTo,
        final boolean snapshotOnly) {
        super(identifier, sequence, replyTo);
        this.snapshotOnly = snapshotOnly;
    }

    AbstractReadTransactionRequest(final T request, final ABIVersion version) {
        super(request, version);
        this.snapshotOnly = request.isSnapshotOnly();
    }

    public final boolean isSnapshotOnly() {
        return snapshotOnly;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("snapshotOnly", snapshotOnly);
    }

    @Override
    protected abstract SerialForm<T> externalizableProxy(ABIVersion version);
}
