/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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

/**
 * Abstract base class for serialization proxies associated with {@link AbstractReadTransactionRequest}s. It implements
 * the initial (Boron) serialization format.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractReadTransactionRequestProxy<T extends AbstractReadTransactionRequest<T>> extends AbstractTransactionRequestProxy<T> {
    private static final long serialVersionUID = 1L;
    private YangInstanceIdentifier path;

    AbstractReadTransactionRequestProxy() {
        // For Externalizable
    }

    AbstractReadTransactionRequestProxy(final T request) {
        super(request);
    }

    @Override
    public final void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        // FIXME: use SerializationUtils.serializePath() here
        out.writeObject(path);
    }

    @Override
    public final void readExternal(final ObjectInput in) throws ClassNotFoundException, IOException {
        super.readExternal(in);
        // FIXME: use SerializationUtils.deserializePath() here
        path = (YangInstanceIdentifier) in.readObject();
    }

    @Override
    protected final T createRequest(final TransactionIdentifier target, final long sequence, final ActorRef replyTo) {
        return createReadRequest(target, sequence, replyTo, path);
    }

    abstract T createReadRequest(TransactionIdentifier target, long sequence, ActorRef replyTo, YangInstanceIdentifier path);
}
