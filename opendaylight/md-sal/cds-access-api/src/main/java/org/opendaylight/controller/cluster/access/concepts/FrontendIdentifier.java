/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A cluster-wide unique identifier of a frontend type located at a cluster member.
 *
 * @author Robert Varga
 */
@Beta
public final class FrontendIdentifier implements WritableIdentifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private MemberName memberName;
        private FrontendType clientType;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final MemberName memberName, final FrontendType clientType) {
            this.memberName = Preconditions.checkNotNull(memberName);
            this.clientType = Preconditions.checkNotNull(clientType);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            memberName.writeTo(out);
            clientType.writeTo(out);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            memberName = MemberName.readFrom(in);
            clientType = FrontendType.readFrom(in);
        }

        private Object readResolve() {
            return new FrontendIdentifier(memberName, clientType);
        }
    }

    private static final long serialVersionUID = 1L;
    private final MemberName memberName;
    private final FrontendType clientType;

    FrontendIdentifier(final MemberName memberName, final FrontendType clientType) {
        this.clientType = Preconditions.checkNotNull(clientType);
        this.memberName = Preconditions.checkNotNull(memberName);
    }

    public static FrontendIdentifier create(final MemberName memberName, final FrontendType clientType) {
        return new FrontendIdentifier(memberName, clientType);
    }

    public static FrontendIdentifier readFrom(final DataInput in) throws IOException {
        final MemberName memberName = MemberName.readFrom(in);
        final FrontendType clientType = FrontendType.readFrom(in);
        return new FrontendIdentifier(memberName, clientType);
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        memberName.writeTo(out);
        clientType.writeTo(out);
    }

    public FrontendType getClientType() {
        return clientType;
    }

    public MemberName getMemberName() {
        return memberName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberName, clientType);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontendIdentifier)) {
            return false;
        }

        final FrontendIdentifier other = (FrontendIdentifier) o;
        return memberName.equals(other.memberName) && clientType.equals(other.clientType);
    }

    public String toPersistentId() {
        return memberName.getName() + "-frontend-" + clientType.getName();
    }

    @Override
    public String toString() {
        return toPersistentId();
    }

    private Object writeReplace() {
        return new Proxy(memberName, clientType);
    }
}
