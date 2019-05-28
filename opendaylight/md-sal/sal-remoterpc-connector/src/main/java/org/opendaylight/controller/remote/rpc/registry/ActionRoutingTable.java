package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketData;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class ActionRoutingTable implements BucketData<ActionRoutingTable>, Serializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "We deal with the field in serialization methods.")
        private Collection<DOMActionInstance> actions;
        private ActorRef rpcInvoker;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ActionRoutingTable table) {
            actions = table.getActions();
            rpcInvoker = table.getRpcInvoker();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(Serialization.serializedActorPath(rpcInvoker));

            final NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(out);
            nnout.writeInt(actions.size());
            for (DOMActionInstance id : actions) {
                nnout.writeSchemaPath(id.getType());
                YangInstanceIdentifier action_path = YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(id.getType().getLastComponent()));
                nnout.writeYangInstanceIdentifier(action_path);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            rpcInvoker = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());

            final NormalizedNodeDataInput nnin = NormalizedNodeInputOutput.newDataInput(in);
            final int size = nnin.readInt();
            actions = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                actions.add(DOMActionInstance.of(nnin.readSchemaPath(), LogicalDatastoreType.OPERATIONAL, nnin.readYangInstanceIdentifier()));
            }
        }

        private Object readResolve() {
            return new ActionRoutingTable(rpcInvoker, actions);
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "We deal with the field in serialization methods.")
    private Collection<DOMActionInstance> actions;
    private final ActorRef rpcInvoker;

    ActionRoutingTable(final ActorRef rpcInvoker, Collection<DOMActionInstance> actions) {
        this.rpcInvoker = Preconditions.checkNotNull(rpcInvoker);
        this.actions = ImmutableSet.copyOf(actions);
    }

    @Override
    public Optional<ActorRef> getWatchActor() {
        return Optional.of(rpcInvoker);
    }

    public Collection<DOMActionInstance> getActions() {
        return actions;
    }

    ActorRef getRpcInvoker() {
        return rpcInvoker;
    }

    ActionRoutingTable updateActions(final Collection<DOMActionInstance> toRemove, final Collection<DOMActionInstance> toAdd) {
        final Set<DOMActionInstance> newActions = new HashSet<>(actions);
        newActions.addAll(toAdd);
        newActions.removeAll(toRemove);
        return new ActionRoutingTable(rpcInvoker, newActions);
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @VisibleForTesting
    boolean contains(final DOMRpcIdentifier routeId) {
        return actions.contains(routeId);
    }

    @VisibleForTesting
    int size() {
        return actions.size();
    }

    @Override
    public String toString() {
        return "ActionRoutingTable{" + "Actions=" + actions + ", rpcInvoker=" + rpcInvoker + '}';
    }
}
