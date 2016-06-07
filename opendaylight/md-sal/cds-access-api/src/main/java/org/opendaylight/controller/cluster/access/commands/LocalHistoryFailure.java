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
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;

/**
 * Generic {@link RequestFailure} involving a {@link LocalHistoryRequest}.
 *
 * @author Robert Varga
 */
@Beta
public final class LocalHistoryFailure extends RequestFailure<LocalHistoryIdentifier, LocalHistoryFailure> {
    private static final long serialVersionUID = 1L;

    LocalHistoryFailure(final LocalHistoryIdentifier target, final RequestException cause) {
        super(target, cause);
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
