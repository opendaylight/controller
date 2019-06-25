/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class ExecuteAction extends AbstractExecute<ContainerNode> {
    private static final long serialVersionUID = 1128904894827335676L;

    private final @NonNull DOMDataTreeIdentifier path;

    private ExecuteAction(final @NonNull SchemaPath type, final @NonNull DOMDataTreeIdentifier path,
            final @Nullable ContainerNode input) {
        // FIXME: do not allow null input
        super(type, input);
        this.path = requireNonNull(path);
    }

    public static @NonNull ExecuteAction from(final @NonNull SchemaPath type, @NonNull final DOMDataTreeIdentifier path,
            final @Nullable ContainerNode input) {
        verify(type.getParent() != null, "Actionss are required to have a multi-element type, %s encountered", type);
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
                // FIXME: deal with data store types?
                stream.writeYangInstanceIdentifier(executeAction.getPath().getRootIdentifier());
                stream.writeOptionalNormalizedNode(executeAction.getInput());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final NormalizedNodeDataInput stream = NormalizedNodeInputOutput.newDataInput(in);
            final SchemaPath name = stream.readSchemaPath();
            final YangInstanceIdentifier path = stream.readYangInstanceIdentifier();
            final NormalizedNode<?, ?> input = stream.readOptionalNormalizedNode().orElse(null);

            executeAction = new ExecuteAction(name, new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, path),
                input);
        }

        private Object readResolve() {
            return verifyNotNull(executeAction);
        }
    }
}
