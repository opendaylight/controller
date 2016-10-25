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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.annotation.RegEx;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * An {@link Identifier} identifying a data store frontend type, which is able to access the data store backend.
 * Frontend implementations need to define this identifier so that multiple clients existing on a member node can be
 * discerned.
 *
 * @author Robert Varga
 */
@Beta
public final class FrontendType implements Comparable<FrontendType>, WritableIdentifier {
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
            return new FrontendType(new String(serialized, StandardCharsets.UTF_8), serialized);
        }
    }

    @RegEx
    private static final String SIMPLE_STRING_REGEX = "^[a-zA-Z0-9-_.*+:=,!~';]+$";
    private static final Pattern SIMPLE_STRING_PATTERN = Pattern.compile(SIMPLE_STRING_REGEX);
    private static final long serialVersionUID = 1L;
    private final String name;

    @SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY",
            justification = "The array elements are non-volatile but we don't access them.")
    private volatile byte[] serialized;

    private FrontendType(final String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    FrontendType(final String name, final byte[] serialized) {
        this(name);
        this.serialized = Verify.verifyNotNull(serialized);
    }

    /**
     * Return a {@link FrontendType} corresponding to a string representation. Input string has constraints
     * on what characters it can contain. It may contain the following:
     * - US-ASCII letters and numbers
     * - special characters: -_.*+:=,!~';
     *
     * @param name the input name
     * @return A {@link FrontendType} instance
     * @throws IllegalArgumentException if the string is null, empty or contains invalid characters
     */
    public static FrontendType forName(final String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(SIMPLE_STRING_PATTERN.matcher(name).matches(),
            "Supplied name %s does not patch pattern %s", name, SIMPLE_STRING_REGEX);
        return new FrontendType(name);
    }

    public static FrontendType readFrom(final DataInput in) throws IOException {
        final byte[] serialized = new byte[in.readInt()];
        in.readFully(serialized);
        return new FrontendType(new String(serialized, StandardCharsets.UTF_8));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        final byte[] local = getSerialized();
        out.writeInt(local.length);
        out.write(local);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof FrontendType && name.equals(((FrontendType)obj).name);
    }

    @Override
    public int compareTo(final FrontendType obj) {
        return this == obj ? 0 : name.compareTo(obj.name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendType.class).add("name", name).toString();
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
