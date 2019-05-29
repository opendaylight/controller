/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public final class ExecuteRpc implements Serializable {
    private static final long serialVersionUID = 1128904894827335676L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final NormalizedNode<?, ?> inputNormalizedNode;
    private final QName rpc;

    private ExecuteRpc(final @Nullable NormalizedNode<?, ?> inputNormalizedNode, final @NonNull QName rpc) {
        this.rpc = requireNonNull(rpc, "rpc Qname should not be null");
        this.inputNormalizedNode = inputNormalizedNode;
    }

    public static ExecuteRpc from(final @NonNull DOMRpcIdentifier rpc, final @Nullable NormalizedNode<?, ?> input) {
        return new ExecuteRpc(input, rpc.getType().getLastComponent());
    }

    public @Nullable NormalizedNode<?, ?> getInputNormalizedNode() {
        return inputNormalizedNode;
    }

    public @NonNull QName getRpc() {
        return rpc;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("rpc", rpc)
                .add("normalizedNode", inputNormalizedNode)
                .toString();
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ExecuteRpc executeRpc;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final ExecuteRpc executeRpc) {
            this.executeRpc = executeRpc;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
                stream.writeQName(executeRpc.getRpc());
                stream.writeOptionalNormalizedNode(executeRpc.getInputNormalizedNode());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final NormalizedNodeDataInput stream = NormalizedNodeInputOutput.newDataInput(in);
            final QName qname = stream.readQName();
            executeRpc = new ExecuteRpc(stream.readOptionalNormalizedNode().orElse(null), qname);
        }

        private Object readResolve() {
            return executeRpc;
        }
    }
}
