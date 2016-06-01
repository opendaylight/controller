/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

/**
 * Type-safe encapsulation of a cluster member name.
 *
 * @author Robert Varga
 */
@Beta
public final class MemberName implements Comparable<MemberName>, WritableIdentifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private byte[] serialized;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            this.serialized = Preconditions.checkNotNull(serialized);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(serialized.length);
            out.write(serialized);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            serialized = new byte[in.readInt()];
            in.readFully(serialized);
        }

        private Object readResolve() {
            // TODO: consider caching instances here
            return new MemberName(new String(serialized, StandardCharsets.UTF_8), serialized);
        }
    }

    private static final long serialVersionUID = 1L;
    private final String name;
    private volatile byte[] serialized;

    private MemberName(final String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    MemberName(final String name, final byte[] serialized) {
        this(name);
        this.serialized = Verify.verifyNotNull(serialized);
    }

    public static MemberName forName(final String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        // TODO: consider caching instances here
        return new MemberName(name);
    }

    public static MemberName readFrom(final DataInput in) throws IOException {
        final byte[] serialized = new byte[in.readInt()];
        in.readFully(serialized);
        return new MemberName(new String(serialized, StandardCharsets.UTF_8));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        final byte[] serialized = getSerialized();
        out.writeInt(serialized.length);
        out.write(serialized);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof MemberName && name.equals(((MemberName)o).name));
    }

    @Override
    public int compareTo(final MemberName o) {
        return this == o ? 0 : name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(MemberName.class).add("name", name).toString();
    }

    private byte[] getSerialized() {
        byte[] local = serialized;
        if (local == null) {
            local = name.getBytes(StandardCharsets.UTF_8);
            serialized = local;
        }
        return local;
    }

    Object writeReplace() {
        return new Proxy(getSerialized());
    }
}
