/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;

/**
 * A {@link RequestFailure} reported when {@link ConnectClientRequest} fails.
 *
 * @author Robert Varga
 */
@Beta
public final class ConnectClientFailure extends RequestFailure<ClientIdentifier, ConnectClientFailure> {
    private static final long serialVersionUID = 1L;

    ConnectClientFailure(final ClientIdentifier target, final RequestException cause) {
        super(target, cause);
    }

    private ConnectClientFailure(final ConnectClientFailure failure, final ABIVersion version) {
        super(failure, version);
    }

    @Override
    protected AbstractRequestFailureProxy<ClientIdentifier, ConnectClientFailure> externalizableProxy(
            final ABIVersion version) {
        return new ConnectClientFailureProxyV1(this);
    }

    @Override
    protected ConnectClientFailure cloneAsVersion(final ABIVersion version) {
        return new ConnectClientFailure(this, version);
    }
}
