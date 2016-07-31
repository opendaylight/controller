/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Serialization proxy for use with {@link ConnectClientFailure}. This class implements initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ConnectClientFailureProxyV1 extends AbstractRequestFailureProxy<ClientIdentifier, ConnectClientFailure> {
    public ConnectClientFailureProxyV1() {
        // For Externalizable
    }

    ConnectClientFailureProxyV1(final ConnectClientFailure failure) {
        super(failure);
    }

    @Override
    protected ConnectClientFailure createFailure(final ClientIdentifier target, final RequestException cause) {
        return new ConnectClientFailure(target, cause);
    }

    @Override
    protected ClientIdentifier readTarget(final DataInput in) throws IOException {
        return ClientIdentifier.readFrom(in);
    }
}
