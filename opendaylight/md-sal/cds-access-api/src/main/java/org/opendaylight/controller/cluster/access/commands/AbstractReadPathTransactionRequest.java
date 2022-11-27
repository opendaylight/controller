/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;

/**
 * Abstract base class for {@link TransactionRequest}s accessing data as visible in the isolated context of a particular
 * transaction. The path of the data being accessed is returned via {@link #getPath()}.
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
public abstract class AbstractReadPathTransactionRequest<T extends AbstractReadPathTransactionRequest<T>>
        extends AbstractReadTransactionRequest<T> {
    interface SerialForm<T extends AbstractReadPathTransactionRequest<T>>
            extends AbstractReadTransactionRequest.SerialForm<T> {

        @Override
        default T readExternal(final ObjectInput in, final TransactionIdentifier target, final long sequence,
                final ActorRef replyTo, final boolean snapshotOnly) throws IOException {
            return readExternal(in, target, sequence, replyTo, snapshotOnly,
                NormalizedNodeDataInput.newDataInput(in).readYangInstanceIdentifier());
        }

        @NonNull T readExternal(@NonNull ObjectInput in, @NonNull TransactionIdentifier target, long sequence,
            @NonNull ActorRef replyTo, boolean snapshotOnly, @NonNull YangInstanceIdentifier path) throws IOException;

        @Override
        default void writeExternal(final ObjectOutput out, final T msg) throws IOException {
            AbstractReadTransactionRequest.SerialForm.super.writeExternal(out, msg);
            try (var nnout = msg.getVersion().getStreamVersion().newDataOutput(out)) {
                nnout.writeYangInstanceIdentifier(msg.getPath());
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private final @NonNull YangInstanceIdentifier path;

    AbstractReadPathTransactionRequest(final TransactionIdentifier identifier, final long sequence,
        final ActorRef replyTo, final YangInstanceIdentifier path, final boolean snapshotOnly) {
        super(identifier, sequence, replyTo, snapshotOnly);
        this.path = requireNonNull(path);
    }

    AbstractReadPathTransactionRequest(final T request, final ABIVersion version) {
        super(request, version);
        this.path = request.getPath();
    }

    public final @NonNull YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("path", path);
    }

    @Override
    protected abstract SerialForm<T> externalizableProxy(ABIVersion version);
}
