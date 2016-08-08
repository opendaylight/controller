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
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Externalizable proxy for use with {@link LocalHistoryFailure}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class LocalHistoryFailureProxyV1 extends AbstractRequestFailureProxy<LocalHistoryIdentifier, LocalHistoryFailure> {
    private static final long serialVersionUID = 1L;

    public LocalHistoryFailureProxyV1() {
        // For Externalizable
    }

    LocalHistoryFailureProxyV1(final LocalHistoryFailure failure) {
        super(failure);
    }

    @Override
    protected LocalHistoryFailure createFailure(final LocalHistoryIdentifier target, final long sequence,
            final RequestException cause) {
        return new LocalHistoryFailure(target, sequence, cause);
    }

    @Override
    protected LocalHistoryIdentifier readTarget(final DataInput in) throws IOException {
        return LocalHistoryIdentifier.readFrom(in);
    }
}
