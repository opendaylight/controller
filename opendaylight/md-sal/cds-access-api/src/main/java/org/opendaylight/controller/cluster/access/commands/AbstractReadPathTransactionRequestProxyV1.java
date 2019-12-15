/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;

/**
 * Abstract base class for serialization proxies associated with {@link AbstractReadTransactionRequest}s. It implements
 * the initial (Boron) serialization format.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractReadPathTransactionRequestProxyV1<T extends AbstractReadPathTransactionRequest<T>>
        extends AbstractReadTransactionRequestProxyV1<T> {
    private static final long serialVersionUID = 1L;

    private YangInstanceIdentifier path;
    private transient NormalizedNodeStreamVersion streamVersion;

    protected AbstractReadPathTransactionRequestProxyV1() {
        // For Externalizable
    }

    AbstractReadPathTransactionRequestProxyV1(final T request) {
        super(request);
        path = request.getPath();
        streamVersion = request.getVersion().getStreamVersion();
    }

    @Override
    public final void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        try (NormalizedNodeDataOutput nnout = streamVersion.newDataOutput(out)) {
            nnout.writeYangInstanceIdentifier(path);
        }
    }

    @Override
    public final void readExternal(final ObjectInput in) throws ClassNotFoundException, IOException {
        super.readExternal(in);
        path = NormalizedNodeDataInput.newDataInput(in).readYangInstanceIdentifier();
    }

    @Override
    protected final T createReadRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo, final boolean snapshotOnly) {
        return createReadPathRequest(target, sequence, replyTo, path, snapshotOnly);
    }

    abstract T createReadPathRequest(TransactionIdentifier target, long sequence, ActorRef replyTo,
            YangInstanceIdentifier requestPath, boolean snapshotOnly);
}
