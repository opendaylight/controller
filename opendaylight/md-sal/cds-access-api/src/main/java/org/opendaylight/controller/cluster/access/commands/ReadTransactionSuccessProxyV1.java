/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Externalizable proxy for use with {@link ReadTransactionSuccess}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ReadTransactionSuccessProxyV1 extends AbstractTransactionSuccessProxy<ReadTransactionSuccess> {
    private static final long serialVersionUID = 1L;
    private Optional<NormalizedNode<?, ?>> data;

    public ReadTransactionSuccessProxyV1() {
        // For Externalizable
    }

    ReadTransactionSuccessProxyV1(final ReadTransactionSuccess request) {
        super(request);
        this.data = request.getData();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        if (data.isPresent()) {
            out.writeBoolean(true);
            try (NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(out)) {
                nnout.writeNormalizedNode(data.get());
            }
        } else {
            out.writeBoolean(false);
        }

        out.writeObject(data);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        if (in.readBoolean()) {
            data = Optional.of(NormalizedNodeInputOutput.newDataInput(in).readNormalizedNode());
        } else {
            data = Optional.absent();
        }
    }

    @Override
    protected ReadTransactionSuccess createSuccess(final TransactionIdentifier target, final long sequence) {
        return new ReadTransactionSuccess(target, sequence, data);
    }
}
