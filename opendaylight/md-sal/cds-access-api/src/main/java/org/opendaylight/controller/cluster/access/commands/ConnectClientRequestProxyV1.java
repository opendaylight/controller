/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * Externalizable proxy for use with {@link ConnectClientRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ConnectClientRequestProxyV1 extends AbstractRequestProxy<ClientIdentifier, ConnectClientRequest>
        implements ConnectClientRequest.SerialForm {
    private static final long serialVersionUID = 8439729661327852159L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public ConnectClientRequestProxyV1() {
        // for Externalizable
    }

    ConnectClientRequestProxyV1(final ConnectClientRequest request) {
        super(request);
    }
}
