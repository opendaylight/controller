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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;

/**
 * A {@link RequestFailure} reported when {@link ConnectClientRequest} fails.
 */
public final class ConnectClientFailure extends RequestFailure<ClientIdentifier, ConnectClientFailure> {
    interface SerialForm extends RequestFailure.SerialForm<ClientIdentifier, ConnectClientFailure> {
        @Override
        default ClientIdentifier readTarget(final DataInput in) throws IOException {
            return ClientIdentifier.readFrom(in);
        }

        @Override
        default ConnectClientFailure createFailure(final ClientIdentifier target, final long sequence,
                final RequestException cause) {
            return new ConnectClientFailure(target, sequence, cause);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    ConnectClientFailure(final ClientIdentifier target, final long sequence, final RequestException cause) {
        super(target, sequence, cause);
    }

    private ConnectClientFailure(final ConnectClientFailure failure, final ABIVersion version) {
        super(failure, version);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return new CCF(this);
    }

    @Override
    protected ConnectClientFailure cloneAsVersion(final ABIVersion version) {
        return new ConnectClientFailure(this, version);
    }
}
