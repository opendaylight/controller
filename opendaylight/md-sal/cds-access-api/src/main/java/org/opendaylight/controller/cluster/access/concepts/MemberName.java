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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Type-safe encapsulation of a cluster member name.
 */
public final class MemberName implements Comparable<MemberName>, WritableIdentifier {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(MemberName.class, "serialized", byte[].class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final @NonNull String name;

    @SuppressWarnings("unused")
    private byte[] serialized;

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

    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.cds.types.rev250131
        .@NonNull MemberName toYang() {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.cds.types.rev250131
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
        final var local = (byte[]) VH.getAcquire(this);
        return local != null ? local : loadSerialized();
    }

    private byte[] loadSerialized() {
        final var bytes = name.getBytes(StandardCharsets.UTF_8);
        // we do not care which bytes are cached
        VH.setRelease(this, bytes);
        return bytes;
    }

    @java.io.Serial
    Object writeReplace() {
        return new MN(getSerialized());
    }
}
