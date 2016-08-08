/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * Externalizable proxy for use with {@link ConnectClientRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ConnectClientRequestProxyV1 extends AbstractRequestProxy<ClientIdentifier, ConnectClientRequest> {
    private ABIVersion minVersion;
    private ABIVersion maxVersion;

    public ConnectClientRequestProxyV1() {
        // for Externalizable
    }

    ConnectClientRequestProxyV1(final ConnectClientRequest request) {
        super(request);
        this.minVersion = request.getMinVersion();
        this.maxVersion = request.getMaxVersion();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        minVersion.writeTo(out);
        maxVersion.writeTo(out);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        minVersion = ABIVersion.inexactReadFrom(in);
        maxVersion = ABIVersion.inexactReadFrom(in);
    }

    @Override
    protected ConnectClientRequest createRequest(final ClientIdentifier target, final long sequence,
            final ActorRef replyTo) {
        return new ConnectClientRequest(target, sequence, replyTo, minVersion, maxVersion);
    }

    @Override
    protected ClientIdentifier readTarget(final DataInput in) throws IOException {
        return ClientIdentifier.readFrom(in);
    }
}
