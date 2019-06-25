/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
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
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;


/**
 * Represents message requesting action invocation.
 */
public final class ExecuteOps implements Serializable {
    private static final long serialVersionUID = 1128904894827335676L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final NormalizedNode<?, ?> inputNormalizedNode;
    private final DOMDataTreeIdentifier path;
    private final QName name;
    private boolean isRpcMessage;


    private ExecuteOps(final @Nullable NormalizedNode<?, ?> inputNormalizedNode,final DOMDataTreeIdentifier path,
                          final @NonNull QName action, boolean isRpcMessage) {
        this.name = requireNonNull(action, " Qname should not be null");
        this.inputNormalizedNode = inputNormalizedNode;
        this.path = path;
        this.isRpcMessage = isRpcMessage;
    }

    /**
     * Generate {@link ExecuteOps} from provided parameters.
     *
     */
    public static ExecuteOps from(final @NonNull QName name,final DOMDataTreeIdentifier path,
                                  final @Nullable NormalizedNode<?, ?> inputNormalizedNode,
                                  final boolean isRpcMessage) {
        return new ExecuteOps(inputNormalizedNode, path, name, isRpcMessage);
    }

    public @Nullable NormalizedNode<?, ?> getInputNormalizedNode() {
        return inputNormalizedNode;
    }

    public @NonNull QName getName() {
        return name;
    }

    public DOMDataTreeIdentifier getPath() {
        return path;
    }


    public @NonNull boolean getIsRpcMessage() {
        return isRpcMessage;
    }

    private Object writeReplace() {
        return new ExecuteOps.Proxy(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("action", name)
                .add("normalizedNode", inputNormalizedNode)
                .toString();
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ExecuteOps executeOps;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(ExecuteOps executeOps) {
            this.executeOps = executeOps;
        }


        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
                stream.writeQName(executeOps.getName());
                stream.writeBoolean(executeOps.isRpcMessage);
                stream.writeOptionalNormalizedNode(executeOps.getInputNormalizedNode());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            final NormalizedNodeDataInput stream = NormalizedNodeInputOutput.newDataInput(in);
            final QName qname = stream.readQName();
            DOMDataTreeIdentifier path = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
                    YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(qname)));
            final Boolean isRpcMessage = stream.readBoolean();
            executeOps = new ExecuteOps(stream.readOptionalNormalizedNode().orElse(null), path, qname, isRpcMessage);
        }

        private Object readResolve() {
            return executeOps;
        }
    }
}
