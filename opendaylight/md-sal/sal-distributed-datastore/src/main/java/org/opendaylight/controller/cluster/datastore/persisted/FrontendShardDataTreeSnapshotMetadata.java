/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.collect.ImmutableList;
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

        public Proxy() {
            // For Externalizable
        }

        Proxy(final FrontendShardDataTreeSnapshotMetadata metadata) {
            this.clients = metadata.getClients();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(clients.size());
            for (FrontendClientMetadata c : clients) {
                c.writeTo(out);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int size = in.readInt();
            final List<FrontendClientMetadata> clients = new ArrayList<>(size);
            for (int i = 0; i < size ; ++i) {
                clients.add(FrontendClientMetadata.readFrom(in));
            }
        }

        private Object readResolve() {
            return new FrontendShardDataTreeSnapshotMetadata(clients);
        }
    }

    private static final long serialVersionUID = 1L;

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
}
