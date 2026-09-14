/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A cluster-wide unique identifier of a frontend type located at a cluster member.
 */
public final class FrontendIdentifier implements WritableIdentifier {
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
        final var memberName = MemberName.readFrom(in);
        final var clientType = FrontendType.readFrom(in);
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
