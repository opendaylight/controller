/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Externalizable proxy for use with {@link LocalHistoryFailure}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class LocalHistoryFailureProxyV1
        extends AbstractRequestFailureProxy<LocalHistoryIdentifier, LocalHistoryFailure>
        implements LocalHistoryFailure.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public LocalHistoryFailureProxyV1() {
        // For Externalizable
    }

    LocalHistoryFailureProxyV1(final LocalHistoryFailure failure) {
        super(failure);
    }
}
