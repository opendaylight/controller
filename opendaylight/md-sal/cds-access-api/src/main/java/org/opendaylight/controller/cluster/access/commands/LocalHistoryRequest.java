/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Abstract base class for {@link Request}s involving specific local history. This class is visible outside of this
 * package solely for the ability to perform a unified instanceof check.
 *
 * @param <T> Message type
 */
public abstract class LocalHistoryRequest<T extends LocalHistoryRequest<T>> extends Request<LocalHistoryIdentifier, T> {
    interface SerialForm<T extends LocalHistoryRequest<T>> extends Request.SerialForm<LocalHistoryIdentifier, T> {
        @Override
        default LocalHistoryIdentifier readTarget(final DataInput in) throws IOException {
            return LocalHistoryIdentifier.readFrom(in);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    LocalHistoryRequest(final LocalHistoryIdentifier target, final long sequence, final ActorRef replyTo) {
        super(target, sequence, replyTo);
        Preconditions.checkArgument(target.getHistoryId() != 0, "History identifier must be non-zero");
    }

    LocalHistoryRequest(final T request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    public final LocalHistoryFailure toRequestFailure(final RequestException cause) {
        return new LocalHistoryFailure(getTarget(), getSequence(), cause);
    }

    @Override
    protected abstract SerialForm<T> externalizableProxy(ABIVersion version);
}
