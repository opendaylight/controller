/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.serialization.JavaSerializer;
import org.apache.pekko.serialization.Serialization;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActionRoutingTable extends AbstractRoutingTable<ActionRoutingTable, DOMActionInstance> {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private static final Logger LOG = LoggerFactory.getLogger(ActionRoutingTable.class);

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

            final NormalizedNodeDataOutput nnout = NormalizedNodeStreamVersion.current().newDataOutput(out);
            nnout.writeInt(actions.size());
            for (DOMActionInstance id : actions) {
                final Absolute type = id.getType();
                nnout.writeSchemaNodeIdentifier(type);
                nnout.writeYangInstanceIdentifier(YangInstanceIdentifier.of(type.lastNodeIdentifier()));
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
                final SchemaNodeIdentifier sni = nnin.readSchemaNodeIdentifier();
                if (!(sni instanceof Absolute absolute)) {
                    throw new InvalidObjectException("Non-absolute type " + sni);
                }

                actions.add(DOMActionInstance.of(absolute, LogicalDatastoreType.OPERATIONAL,
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
