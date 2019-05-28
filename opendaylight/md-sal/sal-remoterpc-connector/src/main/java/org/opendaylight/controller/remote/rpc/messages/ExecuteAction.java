package org.opendaylight.controller.remote.rpc.messages;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

import java.io.*;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class ExecuteAction implements Serializable {
//    private static final long serialVersionUID = 1128904894827335676L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final Optional<ContainerNode> containerNode;
    private final QName action;

    private ExecuteAction(final @Nullable Optional<ContainerNode> containerNode, final @NonNull QName action) {
        this.action = requireNonNull(action, "action Qname should not be null");
        this.containerNode = containerNode;
    }

    public static ExecuteAction from(final @NonNull QName name, final @Nullable Optional<ContainerNode> containerNode) {
        return new ExecuteAction(containerNode, name);
    }

    public @Nullable Optional<ContainerNode> getContainerNode() {
        return containerNode;
    }

    public @NonNull QName getAction() {
        return action;
    }

    private Object writeReplace() {
        return new ExecuteAction.Proxy(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("action", action)
                .add("normalizedNode", containerNode)
                .toString();
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ExecuteAction executeAction;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(ExecuteAction executeAction) {
            this.executeAction = executeAction;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(executeAction.getAction());
            SerializationUtils.serializeNormalizedNode(executeAction.getContainerNode().get(), out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            QName qname = (QName) in.readObject();
            executeAction = new ExecuteAction(Optional.of((ContainerNode) SerializationUtils.deserializeNormalizedNode(in)) , qname);
        }

        private Object readResolve() {
            return executeAction;
        }
    }
}
