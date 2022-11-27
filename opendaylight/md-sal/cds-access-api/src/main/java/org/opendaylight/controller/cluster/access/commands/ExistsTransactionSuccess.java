/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to an {@link ExistsTransactionRequest}. It indicates presence of requested data via
 * {@link #getExists()}.
 */
public final class ExistsTransactionSuccess extends TransactionSuccess<ExistsTransactionSuccess> {
    interface SerialForm extends TransactionSuccess.SerialForm<ExistsTransactionSuccess> {
        @Override
        default ExistsTransactionSuccess readExternal(final ObjectInput in, final TransactionIdentifier target,
                final long sequence) throws IOException {
            return new ExistsTransactionSuccess(target, sequence, in.readBoolean());
        }

        @Override
        default void writeExternal(final ObjectOutput out, final ExistsTransactionSuccess msg) throws IOException {
            out.writeBoolean(msg.getExists());
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final boolean exists;

    private ExistsTransactionSuccess(final ExistsTransactionSuccess success, final ABIVersion version) {
        super(success, version);
        exists = success.exists;
    }

    public ExistsTransactionSuccess(final TransactionIdentifier target, final long sequence, final boolean exists) {
        super(target, sequence);
        this.exists = exists;
    }

    public boolean getExists() {
        return exists;
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new ETS(this) : new ExistsTransactionSuccessProxyV1(this);
    }

    @Override
    protected ExistsTransactionSuccess cloneAsVersion(final ABIVersion version) {
        return new ExistsTransactionSuccess(this, version);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("exists", exists);
    }
}
