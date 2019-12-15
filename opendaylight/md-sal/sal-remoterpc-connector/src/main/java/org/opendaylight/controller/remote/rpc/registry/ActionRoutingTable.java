/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActionRoutingTable extends AbstractRoutingTable<ActionRoutingTable, DOMActionInstance> {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private static final Logger LOG = LoggerFactory.getLogger(ActionRoutingTable.class);

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "We deal with the field in serialization methods.")
        private Collection<DOMActionInstance> actions;
        private ActorRef opsInvoker;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ActionRoutingTable table) {
            actions = table.getItems();
            opsInvoker = table.getInvoker();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            LOG.debug("serializing ActionRoutingTable.");
            out.writeObject(Serialization.serializedActorPath(opsInvoker));

            final NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(out);
            nnout.writeInt(actions.size());
            for (DOMActionInstance id : actions) {
                nnout.writeSchemaPath(id.getType());
                YangInstanceIdentifier actionPath = YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(id.getType().getLastComponent()));
                nnout.writeYangInstanceIdentifier(actionPath);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            LOG.debug("deserializing ActionRoutingTable");
            opsInvoker = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());

            final NormalizedNodeDataInput nnin = NormalizedNodeDataInput.newDataInput(in);
            final int size = nnin.readInt();
            actions = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                actions.add(DOMActionInstance.of(nnin.readSchemaPath(), LogicalDatastoreType.OPERATIONAL,
                        nnin.readYangInstanceIdentifier()));
            }
        }

        private Object readResolve() {
            return new ActionRoutingTable(opsInvoker, actions);
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ActionRoutingTable.class);

    ActionRoutingTable(final ActorRef invoker, final Collection<DOMActionInstance> actions) {
        super(invoker, actions);
    }

    ActionRoutingTable updateActions(final Collection<DOMActionInstance> toAdd,
                                     final Collection<DOMActionInstance> toRemove) {
        LOG.debug("Updating actions in ActionRoutingTable");
        final Set<DOMActionInstance> newActions = new HashSet<>(getItems());
        newActions.addAll(toAdd);
        newActions.removeAll(toRemove);
        return new ActionRoutingTable(getInvoker(), newActions);
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }
}
