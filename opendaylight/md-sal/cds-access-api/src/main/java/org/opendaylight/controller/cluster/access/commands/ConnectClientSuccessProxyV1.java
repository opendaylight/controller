/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.AbstractSuccessProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * Externalizable proxy for use with {@link ConnectClientSuccess}. It implements the initial (Boron) serialization
 * format.
 */
@Deprecated(since = "7.0.0", forRemoval = true)
final class ConnectClientSuccessProxyV1 extends AbstractSuccessProxy<ClientIdentifier, ConnectClientSuccess>
        implements ConnectClientSuccess.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public ConnectClientSuccessProxyV1() {
        // For Externalizable
    }

    ConnectClientSuccessProxyV1(final ConnectClientSuccess success) {
        super(success);
    }
}
