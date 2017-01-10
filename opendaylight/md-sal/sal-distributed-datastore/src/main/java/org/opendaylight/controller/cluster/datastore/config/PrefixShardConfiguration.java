/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Configuration for prefix based shards.
 */
public class PrefixShardConfiguration implements Serializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private PrefixShardConfiguration prefixShardConfiguration;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final PrefixShardConfiguration prefixShardConfiguration) {
            this.prefixShardConfiguration = prefixShardConfiguration;
        }

        @Override
        public void writeExternal(final ObjectOutput objectOutput) throws IOException {
            objectOutput.writeObject(prefixShardConfiguration.getPrefix());
            objectOutput.writeObject(prefixShardConfiguration.getShardStrategyName());

            objectOutput.writeInt(prefixShardConfiguration.getShardMemberNames().size());
            for (MemberName name : prefixShardConfiguration.getShardMemberNames()) {
                name.writeTo(objectOutput);
            }
        }

        @Override
        public void readExternal(final ObjectInput objectInput) throws IOException, ClassNotFoundException {
            final DOMDataTreeIdentifier prefix =  (DOMDataTreeIdentifier) objectInput.readObject();
            final String strategyName = (String) objectInput.readObject();

            final int size = objectInput.readInt();
            final Collection<MemberName> shardMemberNames = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                shardMemberNames.add(MemberName.readFrom(objectInput));
            }

            prefixShardConfiguration = new PrefixShardConfiguration(prefix, strategyName, shardMemberNames);
        }

        private Object readResolve() {
            return prefixShardConfiguration;
        }
    }

    private static final long serialVersionUID = 1L;

    private final DOMDataTreeIdentifier prefix;
    private final String shardStrategyName;
    private final Collection<MemberName> shardMemberNames;

    public PrefixShardConfiguration(final DOMDataTreeIdentifier prefix,
                                    final String shardStrategyName,
                                    final Collection<MemberName> shardMemberNames) {
        this.prefix = Preconditions.checkNotNull(prefix);
        this.shardStrategyName = Preconditions.checkNotNull(shardStrategyName);
        this.shardMemberNames = ImmutableSet.copyOf(shardMemberNames);
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    public String getShardStrategyName() {
        return shardStrategyName;
    }

    public Collection<MemberName> getShardMemberNames() {
        return shardMemberNames;
    }

    @Override
    public String toString() {
        return "PrefixShardConfiguration{"
                + "prefix=" + prefix
                + ", shardStrategyName='"
                + shardStrategyName + '\''
                + ", shardMemberNames=" + shardMemberNames
                + '}';
    }

    private Object writeReplace() {
        return new Proxy(this);
    }
}
