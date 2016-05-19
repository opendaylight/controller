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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * A cluster-wide unique identifier of a frontend type located at a cluster member.
 *
 * @author Robert Varga
 */
@Beta
public final class FrontendIdentifier<T extends FrontendType> implements Identifier {
    private static final class Proxy<T extends FrontendType> implements Externalizable {
        private static final long serialVersionUID = 1L;
        private MemberName memberName;
        private T clientType;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final MemberName memberName, final T clientType) {
            this.memberName = Preconditions.checkNotNull(memberName);
            this.clientType = Preconditions.checkNotNull(clientType);
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(memberName);
            out.writeObject(clientType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            memberName = (MemberName) in.readObject();
            clientType = (T) in.readObject();
        }

        private Object readResolve() {
            return new FrontendIdentifier<>(memberName, clientType);
        }
    }

    private static final long serialVersionUID = 1L;
    private final MemberName memberName;
    private final T clientType;

    FrontendIdentifier(final MemberName memberName, final T clientType) {
        this.clientType = Preconditions.checkNotNull(clientType);
        this.memberName = Preconditions.checkNotNull(memberName);
    }

    public static <T extends FrontendType> FrontendIdentifier<T> create(MemberName memberName, final T clientType) {
        return new FrontendIdentifier<>(memberName, clientType);
    }

    public T getClientType() {
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

        final FrontendIdentifier<?> other = (FrontendIdentifier<?>) o;
        return memberName.equals(other.memberName) && clientType.equals(other.clientType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendIdentifier.class).add("member", memberName)
                .add("clientType", clientType).toString();
    }

    private Object writeReplace() {
        return new Proxy<>(memberName, clientType);
    }
}
