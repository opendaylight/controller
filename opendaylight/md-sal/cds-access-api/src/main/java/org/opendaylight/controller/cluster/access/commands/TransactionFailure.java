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
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Generic {@link RequestFailure} involving a {@link TransactionRequest}.
 *
 * @author Robert Varga
 */
@Beta
public final class TransactionFailure extends RequestFailure<TransactionIdentifier, TransactionFailure> {
    private static final long serialVersionUID = 1L;

    TransactionFailure(final TransactionIdentifier target, final long sequence, final RequestException cause) {
        super(target, sequence, cause);
    }

    @Override
    protected TransactionFailure cloneAsVersion(final ABIVersion version) {
        return this;
    }

    @Override
    protected TransactionFailureProxyV1 externalizableProxy(final ABIVersion version) {
        return new TransactionFailureProxyV1(this);
    }
}
