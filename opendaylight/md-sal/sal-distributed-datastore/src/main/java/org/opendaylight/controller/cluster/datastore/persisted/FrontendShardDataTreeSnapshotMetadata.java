/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class FrontendShardDataTreeSnapshotMetadata extends
        ShardDataTreeSnapshotMetadata<FrontendShardDataTreeSnapshotMetadata> {

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private List<FrontendClientMetadata> clients;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final FrontendShardDataTreeSnapshotMetadata metadata) {
            this.clients = metadata.getClients();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(clients.size());
            for (final FrontendClientMetadata c : clients) {
                c.writeTo(out);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int size = in.readInt();
            final List<FrontendClientMetadata> readedClients = new ArrayList<>(size);
            for (int i = 0; i < size ; ++i) {
                readedClients.add(FrontendClientMetadata.readFrom(in));
            }
            this.clients = ImmutableList.copyOf(readedClients);
        }

        private Object readResolve() {
            return new FrontendShardDataTreeSnapshotMetadata(clients);
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final List<FrontendClientMetadata> clients;

    public FrontendShardDataTreeSnapshotMetadata(final Collection<FrontendClientMetadata> clients) {
        this.clients = ImmutableList.copyOf(clients);
    }

    public List<FrontendClientMetadata> getClients() {
        return clients;
    }

    @Override
    protected Externalizable externalizableProxy() {
        return new Proxy(this);
    }

    @Override
    public Class<FrontendShardDataTreeSnapshotMetadata> getType() {
        return FrontendShardDataTreeSnapshotMetadata.class;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendShardDataTreeSnapshotMetadata.class).add("clients", clients)
                .toString();
    }
}
