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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public class RpcResponse extends AbstractResponse {
    private static final long serialVersionUID = -4211279498688989245L;

    public RpcResponse(final @Nullable ContainerNode output) {
        super(output);
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
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
            SerializationUtils.writeNormalizedNode(out, rpcResponse.getOutput());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            rpcResponse = new RpcResponse((ContainerNode) SerializationUtils.readNormalizedNode(in).orElse(null));
        }

        private Object readResolve() {
            return rpcResponse;
        }
    }
}
