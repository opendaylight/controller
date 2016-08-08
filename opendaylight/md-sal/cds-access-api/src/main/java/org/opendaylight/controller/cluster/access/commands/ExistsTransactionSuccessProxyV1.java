/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link ExistsTransactionSuccess}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ExistsTransactionSuccessProxyV1 extends AbstractTransactionSuccessProxy<ExistsTransactionSuccess> {
    private static final long serialVersionUID = 1L;
    private boolean exists;

    public ExistsTransactionSuccessProxyV1() {
        // For Externalizable
    }

    ExistsTransactionSuccessProxyV1(final ExistsTransactionSuccess request) {
        super(request);
        this.exists = request.getExists();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeBoolean(exists);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        exists = in.readBoolean();
    }

    @Override
    protected ExistsTransactionSuccess createSuccess(final TransactionIdentifier target, final long sequence) {
        return new ExistsTransactionSuccess(target, sequence, exists);
    }
}
