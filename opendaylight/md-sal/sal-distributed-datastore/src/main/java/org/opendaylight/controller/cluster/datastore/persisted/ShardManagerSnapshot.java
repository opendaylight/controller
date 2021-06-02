/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Represents the persisted snapshot state for the ShardManager.
 *
 * @author Thomas Pantelis
 */
public class ShardManagerSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ShardManagerSnapshot snapshot;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ShardManagerSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(snapshot.shardList.size());
            for (String shard: snapshot.shardList) {
                out.writeObject(shard);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            int size = in.readInt();
            List<String> localShardList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                localShardList.add((String) in.readObject());
            }

            snapshot = new ShardManagerSnapshot(localShardList);
        }

        private Object readResolve() {
            return snapshot;
        }
    }

    private final List<String> shardList;

    public ShardManagerSnapshot(final @NonNull List<String> shardList) {
        this.shardList = ImmutableList.copyOf(shardList);
    }

    public List<String> getShardList() {
        return this.shardList;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return "ShardManagerSnapshot [ShardList = " + shardList + " ]";
    }
}
