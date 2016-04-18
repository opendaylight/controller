/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cds.access;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

public final class FrontendIdentifier implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private MemberName memberName;
        private long generation;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final MemberName memberName, final long generation) {
            this.memberName = Preconditions.checkNotNull(memberName);
            this.generation = generation;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(memberName.getName());
            out.writeLong(generation);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            final String str = (String) in.readObject();
            memberName = MemberName.forString(str);
            generation = in.readLong();
        }

        @SuppressWarnings("unused")
        private FrontendIdentifier readResolve() {
            return new FrontendIdentifier(memberName, generation);
        }
    }

    private final MemberName memberName;
    private final long generation;

    FrontendIdentifier(final MemberName memberName, long generation) {
        this.memberName = Preconditions.checkNotNull(memberName);
        this.generation = generation;
    }

    public static FrontendIdentifier create(final MemberName memberName, final long generation) {
        return new FrontendIdentifier(memberName, generation);
    }

    @Override
    public int hashCode() {
        return memberName.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        return o instanceof FrontendIdentifier && memberName.equals(((FrontendIdentifier)o).memberName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendIdentifier.class).add("memberName", memberName)
                .add("generation", generation).toString();
    }

    @SuppressWarnings("unused")
    private Proxy writeReplace() {
        return new Proxy(memberName, generation);
    }
}
