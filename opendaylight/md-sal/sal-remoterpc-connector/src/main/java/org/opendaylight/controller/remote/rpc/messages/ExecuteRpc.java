/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class ExecuteRpc extends AbstractExecute<@Nullable NormalizedNode<?, ?>> {
    private static final long serialVersionUID = 1128904894827335676L;

    private ExecuteRpc(final @NonNull SchemaPath type, final @Nullable NormalizedNode<?, ?> input) {
        super(type, input);
    }

    public static @NonNull ExecuteRpc from(final @NonNull DOMRpcIdentifier rpc,
            final @Nullable NormalizedNode<?, ?> input) {
        return new ExecuteRpc(rpc.getType(), input);
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ExecuteRpc executeRpc;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {

        }

        Proxy(final ExecuteRpc executeRpc) {
            this.executeRpc = requireNonNull(executeRpc);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
                stream.writeQName(executeRpc.getType().getLastComponent());
                stream.writeOptionalNormalizedNode(executeRpc.getInput());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            final NormalizedNodeDataInput stream = NormalizedNodeDataInput.newDataInput(in);
            final SchemaPath type = SchemaPath.ROOT.createChild(stream.readQName());
            final NormalizedNode<?, ?> input = stream.readOptionalNormalizedNode().orElse(null);
            executeRpc = new ExecuteRpc(type, input);
        }

        private Object readResolve() {
            return executeRpc;
        }
    }
}
