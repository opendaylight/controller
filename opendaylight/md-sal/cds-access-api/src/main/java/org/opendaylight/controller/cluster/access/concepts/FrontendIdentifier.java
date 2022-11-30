/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A cluster-wide unique identifier of a frontend type located at a cluster member.
 */
public final class FrontendIdentifier implements WritableIdentifier {
    interface SerialForm extends Externalizable {
        @NonNull FrontendIdentifier identifier();

        void setIdentifier(@NonNull FrontendIdentifier identifier);

        @java.io.Serial
        Object readResolve();

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var id = identifier();
            id.memberName.writeTo(out);
            id.clientType.writeTo(out);
        }

        @Override
        default void readExternal(final ObjectInput in) throws IOException {
            setIdentifier(new FrontendIdentifier(MemberName.readFrom(in), FrontendType.readFrom(in)));
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy implements SerialForm {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private FrontendIdentifier identifier;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final FrontendIdentifier identifier) {
            this.identifier = requireNonNull(identifier);
        }

        @Override
        public FrontendIdentifier identifier() {
            return verifyNotNull(identifier);
        }

        @Override
        public void setIdentifier(final FrontendIdentifier identifier) {
            this.identifier = requireNonNull(identifier);
        }

        @Override
        public Object readResolve() {
            return identifier();
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final MemberName memberName;
    private final FrontendType clientType;

    FrontendIdentifier(final MemberName memberName, final FrontendType clientType) {
        this.clientType = requireNonNull(clientType);
        this.memberName = requireNonNull(memberName);
    }

    public static @NonNull FrontendIdentifier create(final MemberName memberName, final FrontendType clientType) {
        return new FrontendIdentifier(memberName, clientType);
    }

    public static @NonNull FrontendIdentifier readFrom(final DataInput in) throws IOException {
        final MemberName memberName = MemberName.readFrom(in);
        final FrontendType clientType = FrontendType.readFrom(in);
        return new FrontendIdentifier(memberName, clientType);
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        memberName.writeTo(out);
        clientType.writeTo(out);
    }

    public @NonNull FrontendType getClientType() {
        return clientType;
    }

    public @NonNull MemberName getMemberName() {
        return memberName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberName, clientType);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof FrontendIdentifier other && memberName.equals(other.memberName)
            && clientType.equals(other.clientType);
    }

    public @NonNull String toPersistentId() {
        return memberName.getName() + "-frontend-" + clientType.getName();
    }

    @Override
    public String toString() {
        return toPersistentId();
    }

    @java.io.Serial
    private Object writeReplace() {
        return new FI(this);
    }
}
