/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class ExecuteAction extends AbstractExecute<@NonNull ContainerNode> {
    private static final long serialVersionUID = 1128904894827335676L;

    private final @NonNull DOMDataTreeIdentifier path;

    private ExecuteAction(final @NonNull SchemaPath type, final @NonNull DOMDataTreeIdentifier path,
            final @NonNull ContainerNode input) {
        super(type, requireNonNull(input));
        this.path = requireNonNull(path);
    }

    public static @NonNull ExecuteAction from(final @NonNull SchemaPath type, @NonNull final DOMDataTreeIdentifier path,
            final @NonNull ContainerNode input) {
        return new ExecuteAction(type, path, input);
    }

    public @NonNull DOMDataTreeIdentifier getPath() {
        return path;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper.add("path", path));
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ExecuteAction executeAction;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {

        }

        Proxy(final ExecuteAction executeAction) {
            this.executeAction = requireNonNull(executeAction);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
                stream.writeSchemaPath(executeAction.getType());
                executeAction.getPath().getDatastoreType().writeTo(out);
                stream.writeYangInstanceIdentifier(executeAction.getPath().getRootIdentifier());
                stream.writeOptionalNormalizedNode(executeAction.getInput());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            final NormalizedNodeDataInput stream = NormalizedNodeDataInput.newDataInput(in);
            final SchemaPath name = stream.readSchemaPath();
            final LogicalDatastoreType type = LogicalDatastoreType.readFrom(in);
            final YangInstanceIdentifier path = stream.readYangInstanceIdentifier();
            final ContainerNode input = (ContainerNode) stream.readOptionalNormalizedNode().orElse(null);

            executeAction = new ExecuteAction(name, new DOMDataTreeIdentifier(type, path), input);
        }

        private Object readResolve() {
            return verifyNotNull(executeAction);
        }
    }
}
