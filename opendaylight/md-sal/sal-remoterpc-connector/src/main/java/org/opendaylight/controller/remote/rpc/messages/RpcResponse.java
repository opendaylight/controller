/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;

public class RpcResponse extends AbstractResponse<ContainerNode> {
    @java.io.Serial
    private static final long serialVersionUID = -4211279498688989245L;

    public RpcResponse(final @Nullable ContainerNode output) {
        super(output);
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }

    static final @Nullable ContainerNode unmaskContainer(final @Nullable NormalizedNode node)
            throws InvalidObjectException {
        return switch (node) {
            case null -> null;
            case ContainerNode container -> container;
            default ->  throw new InvalidObjectException("Unexpected data " + node.contract().getSimpleName());
        };
    }

    static final @Nullable ContainerNode readContainerNode(final ObjectInput in) throws IOException {
        return in.readBoolean() ? unmaskContainer(NormalizedNodeDataInput.newDataInput(in).readNormalizedNode()) : null;
    }

    static final void writeOutput(final ObjectOutput out, final @Nullable ContainerNode output) throws IOException {
        if (output != null) {
            out.writeBoolean(true);
            try (var stream = NormalizedNodeStreamVersion.current().newDataOutput(out)) {
                stream.writeNormalizedNode(output);
            }
        } else {
            out.writeBoolean(false);
        }
    }

    private static class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private RpcResponse rpcResponse;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final RpcResponse rpcResponse) {
            this.rpcResponse = rpcResponse;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            writeOutput(out, rpcResponse.getOutput());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            rpcResponse = new RpcResponse(readContainerNode(in));
        }

        private Object readResolve() {
            return rpcResponse;
        }
    }
}
