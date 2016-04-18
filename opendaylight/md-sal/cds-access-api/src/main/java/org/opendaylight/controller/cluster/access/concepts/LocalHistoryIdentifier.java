/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.yangtools.concepts.Identifier;

public final class LocalHistoryIdentifier implements Identifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private FrontendIdentifier frontendId;
        private long historyId;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final FrontendIdentifier frontendId, final long historyId) {
            this.frontendId = Preconditions.checkNotNull(frontendId);
            this.historyId = historyId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(frontendId);
            out.writeLong(historyId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            frontendId = (FrontendIdentifier) in.readObject();
            historyId = in.readLong();
        }

        @SuppressWarnings("unused")
        private LocalHistoryIdentifier readResolve() {
            return new LocalHistoryIdentifier(frontendId, historyId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final FrontendIdentifier frontendId;
    private final long historyId;

    public LocalHistoryIdentifier(final FrontendIdentifier frontendId, final long historyId) {
        this.frontendId = Preconditions.checkNotNull(frontendId);
        this.historyId = historyId;
    }

    public FrontendIdentifier getFrontendId() {
        return frontendId;
    }

    public long getHistoryId() {
        return historyId;
    }

    @Override
    public int hashCode() {
        return frontendId.hashCode() * 31 + Long.hashCode(historyId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocalHistoryIdentifier)) {
            return false;
        }

        final LocalHistoryIdentifier other = (LocalHistoryIdentifier) o;
        return historyId == other.historyId && frontendId.equals(other.frontendId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(CreateLocalHistoryRequest.class).add("frontend", frontendId)
                .add("history", historyId).toString();
    }

    @SuppressWarnings("unused")
    private Proxy writeReplace() {
        return new Proxy(frontendId, historyId);
    }
}
