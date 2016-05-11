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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Type-safe encapsulation of a cluster member name.
 *
 * @author Robert Varga
 */
@Beta
public final class MemberName implements Comparable<MemberName>, Identifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private byte[] serialized;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final String name) {
            serialized = name.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(serialized.length);
            out.write(serialized);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            serialized = new byte[in.readInt()];
            in.readFully(serialized);
        }

        private Object readResolve() {
            // TODO: consider caching instances here
            return new MemberName(new String(serialized, StandardCharsets.UTF_8), this);
        }
    }

    private static final long serialVersionUID = 1L;
    private final String name;
    private volatile Proxy proxy;

    MemberName(final String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    MemberName(final String name, final Proxy proxy) {
        this.name = Preconditions.checkNotNull(name);
        this.proxy = Verify.verifyNotNull(proxy);
    }

    public static MemberName forName(final String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        // TODO: consider caching instances here
        return new MemberName(name);
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

    Object writeReplace() {
        Proxy ret = proxy;
        if (ret == null) {
            // We do not really care if multiple threads race here
            ret = new Proxy(name);
            proxy = ret;
        }

        return ret;
    }
}
