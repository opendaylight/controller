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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successuful reply to an {@link ExistsTransactionRequest}. It indicates presence of requested data via
 * {@link #getExists()}.
 *
 * @author Robert Varga
 */
@Beta
public final class ExistsTransactionSuccess extends TransactionSuccess<ExistsTransactionSuccess> {
    private static final long serialVersionUID = 1L;
    private final boolean exists;

    public ExistsTransactionSuccess(final TransactionIdentifier target, final long sequence, final boolean exists) {
        super(target, sequence);
        this.exists = exists;
    }

    public boolean getExists() {
        return exists;
    }

    @Override
    protected ExistsTransactionSuccessProxy externalizableProxy(final ABIVersion version) {
        return new ExistsTransactionSuccessProxy(this);
    }

    @Override
    protected ExistsTransactionSuccess cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
