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
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
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
 * It also includes request stream sequencing information.
 *
 * @author Robert Varga
 */
@Beta
public final class ConnectClientRequest extends Request<ClientIdentifier, ConnectClientRequest> {
    private static final long serialVersionUID = 1L;

    private final ABIVersion minVersion;
    private final ABIVersion maxVersion;
    private final long resumeSequence;

    public ConnectClientRequest(final ClientIdentifier identifier, final ActorRef replyTo, final ABIVersion minVersion,
            final ABIVersion maxVersion) {
        this(identifier, replyTo, minVersion, maxVersion, 0);
    }

    public ConnectClientRequest(final ClientIdentifier identifier, final ActorRef replyTo, final ABIVersion minVersion,
            final ABIVersion maxVersion, final long resumeSequence) {
        super(identifier, replyTo);
        this.minVersion = Preconditions.checkNotNull(minVersion);
        this.maxVersion = Preconditions.checkNotNull(maxVersion);
        this.resumeSequence = resumeSequence;
    }

    private ConnectClientRequest(final ConnectClientRequest request, final ABIVersion version) {
        super(request, version);
        this.minVersion = request.minVersion;
        this.maxVersion = request.maxVersion;
        this.resumeSequence = request.resumeSequence;
    }

    public ABIVersion getMinVersion() {
        return minVersion;
    }

    public ABIVersion getMaxVersion() {
        return maxVersion;
    }

    public long getResumeSequence() {
        return resumeSequence;
    }

    @Override
    public final ConnectClientFailure toRequestFailure(final RequestException cause) {
        return new ConnectClientFailure(getTarget(), cause);
    }

    @Override
    protected AbstractRequestProxy<ClientIdentifier, ConnectClientRequest> externalizableProxy(final ABIVersion version) {
        return new ConnectClientRequestProxyV1(this);
    }

    @Override
    protected ConnectClientRequest cloneAsVersion(final ABIVersion version) {
        return new ConnectClientRequest(this, version);
    }

    @Override
    protected @Nonnull ToStringHelper addToStringAttributes(final @Nonnull ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("minVersion", minVersion).add("maxVersion", maxVersion)
                .add("resumeSequence", resumeSequence);
    }
}
