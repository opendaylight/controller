package org.opendaylight.controller.remote.rpc.messages;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Optional;

public class ActionResponse implements Serializable {
    private static final long serialVersionUID = -4211279498688989245L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final Optional<ContainerNode> resultContainerNode;

    public ActionResponse(final Optional<ContainerNode> containerNode) {
        resultContainerNode = containerNode;
    }

    public @Nullable Optional<ContainerNode> getResultContainerNode() {
        return resultContainerNode;
    }

    private Object writeReplace() {
        return new ActionResponse.Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ActionResponse actionResponse;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(ActionResponse actionResponse) {
            this.actionResponse = actionResponse;
        }

        @Override
        public void writeExternal(ObjectOutput out) {
            SerializationUtils.serializeNormalizedNode(actionResponse.getResultContainerNode().get(), out);
        }

        @Override
        public void readExternal(ObjectInput in) {
            actionResponse = new ActionResponse(Optional.of((ContainerNode) SerializationUtils.deserializeNormalizedNode(in)));
        }

        private Object readResolve() {
            return actionResponse;
        }
    }
}
