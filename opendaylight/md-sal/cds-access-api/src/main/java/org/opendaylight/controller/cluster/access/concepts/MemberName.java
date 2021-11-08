/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

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

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            this.serialized = requireNonNull(serialized);
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

    private final @NonNull String name;

    @SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY",
            justification = "The array elements are non-volatile but we don't access them.")
    private volatile byte[] serialized;

    private MemberName(final String name) {
        this.name = requireNonNull(name);
    }

    MemberName(final String name, final byte[] serialized) {
        this(name);
        this.serialized = verifyNotNull(serialized);
    }

    public static @NonNull MemberName forName(final String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        // TODO: consider caching instances here
        return new MemberName(name);
    }

    public static @NonNull MemberName readFrom(final DataInput in) throws IOException {
        final byte[] serialized = new byte[in.readInt()];
        in.readFully(serialized);
        return new MemberName(new String(serialized, StandardCharsets.UTF_8));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        final byte[] local = getSerialized();
        out.writeInt(local.length);
        out.write(local);
    }

    public @NonNull String getName() {
        return name;
    }

    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.cds.types.rev191024
        .@NonNull MemberName toYang() {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.cds.types.rev191024
                .MemberName(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof MemberName && name.equals(((MemberName)obj).name);
    }

    @Override
    public int compareTo(final MemberName obj) {
        return this == obj ? 0 : name.compareTo(obj.name);
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
