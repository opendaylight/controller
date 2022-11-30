/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

/**
 * Externalizable proxy for use with {@link PurgeLocalHistoryRequest}. It implements the initial (Boron) serialization
 * format.
 */
@Deprecated(since = "7.0.0", forRemoval = true)
final class PurgeLocalHistoryRequestProxyV1 extends AbstractLocalHistoryRequestProxy<PurgeLocalHistoryRequest>
        implements PurgeLocalHistoryRequest.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public PurgeLocalHistoryRequestProxyV1() {
        // For Externalizable
    }

    PurgeLocalHistoryRequestProxyV1(final PurgeLocalHistoryRequest request) {
        super(request);
    }
}
