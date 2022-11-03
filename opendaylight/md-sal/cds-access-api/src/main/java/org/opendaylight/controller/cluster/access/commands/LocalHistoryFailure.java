/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.Serial;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;

/**
 * Generic {@link RequestFailure} involving a {@link LocalHistoryRequest}.
 */
public final class LocalHistoryFailure extends RequestFailure<LocalHistoryIdentifier, LocalHistoryFailure> {
    @Serial
    private static final long serialVersionUID = 1L;

    LocalHistoryFailure(final LocalHistoryIdentifier target, final long sequence, final RequestException cause) {
        super(target, sequence, cause);
    }

    @Override
    protected LocalHistoryFailure cloneAsVersion(final ABIVersion version) {
        return this;
    }

    @Override
    protected LocalHistoryFailureProxyV1 externalizableProxy(final ABIVersion version) {
        return new LocalHistoryFailureProxyV1(this);
    }
}
