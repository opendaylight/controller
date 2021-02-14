/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@SuppressFBWarnings({"SE_TRANSIENT_FIELD_NOT_RESTORED", "DMI_NONSERIALIZABLE_OBJECT_WRITTEN"})
public class ActionResponse extends AbstractResponse<ContainerNode> {
    private static final long serialVersionUID = 1L;

    private final transient @NonNull ImmutableList<@NonNull RpcError> errors;

    public ActionResponse(final @NonNull Optional<ContainerNode> output, @NonNull final Collection<RpcError> errors) {
        super(output.orElse(null));
        this.errors = ImmutableList.copyOf(errors);
    }

    public @NonNull ImmutableList<@NonNull RpcError> getErrors() {
        return errors;
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ActionResponse actionResponse;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final ActionResponse actionResponse) {
            this.actionResponse = requireNonNull(actionResponse);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(actionResponse.getErrors());
            SerializationUtils.writeNormalizedNode(out, actionResponse.getOutput());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            @SuppressWarnings("unchecked")
            final ImmutableList<RpcError> errors = (ImmutableList<RpcError>) in.readObject();
            final Optional<NormalizedNode> output = SerializationUtils.readNormalizedNode(in);
            actionResponse = new ActionResponse(output.map(ContainerNode.class::cast), errors);
        }

        private Object readResolve() {
            return actionResponse;
        }
    }
}
