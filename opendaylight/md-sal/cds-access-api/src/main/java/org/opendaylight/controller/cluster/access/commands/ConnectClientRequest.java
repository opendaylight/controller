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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
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
    protected AbstractRequestProxy<ClientIdentifier, ConnectClientRequest> externalizableProxy(
            final ABIVersion version) {
        return new ConnectClientRequestProxyV1(this);
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
