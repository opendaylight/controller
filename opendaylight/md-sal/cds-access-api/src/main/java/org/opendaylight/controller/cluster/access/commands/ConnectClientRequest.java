/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Request to connect a frontend instance to the backend. It carries basic information about the frontend:
 * - its coordinates in {@link #getReplyTo()}.
 * - its minimum supported ABI version
 * - its maximum supported ABI version
 *
 * <p>
 * It also includes request stream sequencing information.
 */
public final class ConnectClientRequest extends Request<ClientIdentifier, ConnectClientRequest> {
    interface SerialForm extends Request.SerialForm<ClientIdentifier, ConnectClientRequest> {
        @Override
        default ConnectClientRequest readExternal(final ObjectInput in, final ClientIdentifier target,
                final long sequence, final ActorRef replyTo) throws IOException {
            return new ConnectClientRequest(target, sequence, replyTo, ABIVersion.inexactReadFrom(in),
                ABIVersion.inexactReadFrom(in));
        }

        @Override
        default ClientIdentifier readTarget(final DataInput in) throws IOException {
            return ClientIdentifier.readFrom(in);
        }

        @Override
        default void writeExternal(final ObjectOutput out, final ConnectClientRequest msg) throws IOException {
            Request.SerialForm.super.writeExternal(out, msg);
            msg.getMinVersion().writeTo(out);
            msg.getMaxVersion().writeTo(out);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private final ABIVersion minVersion;
    private final ABIVersion maxVersion;

    ConnectClientRequest(final ClientIdentifier identifier, final long txSequence, final ActorRef replyTo,
            final ABIVersion minVersion, final ABIVersion maxVersion) {
        super(identifier, txSequence, replyTo);
        this.minVersion = requireNonNull(minVersion);
        this.maxVersion = requireNonNull(maxVersion);
    }

    public ConnectClientRequest(final ClientIdentifier identifier, final ActorRef replyTo, final ABIVersion minVersion,
            final ABIVersion maxVersion) {
        this(identifier, 0, replyTo, minVersion, maxVersion);
    }

    private ConnectClientRequest(final ConnectClientRequest request, final ABIVersion version) {
        super(request, version);
        minVersion = request.minVersion;
        maxVersion = request.maxVersion;
    }

    public ABIVersion getMinVersion() {
        return minVersion;
    }

    public ABIVersion getMaxVersion() {
        return maxVersion;
    }

    @Override
    public ConnectClientFailure toRequestFailure(final RequestException cause) {
        return new ConnectClientFailure(getTarget(), getSequence(), cause);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lte(version) ? new ConnectClientRequestProxyV1(this) : new CCR(this);
    }

    @Override
    protected ConnectClientRequest cloneAsVersion(final ABIVersion version) {
        return new ConnectClientRequest(this, version);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("minVersion", minVersion).add("maxVersion", maxVersion);
    }
}
