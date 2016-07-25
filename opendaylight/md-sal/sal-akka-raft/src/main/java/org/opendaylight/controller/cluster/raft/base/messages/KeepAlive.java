/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * Message sent by the leader's keep-alive actor to its followers to prevent election time outs.
 *
 * @author Thomas Pantelis
 */
public class KeepAlive implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String leaderId;

    public KeepAlive(@Nonnull String leaderId) {
        this.leaderId = Preconditions.checkNotNull(leaderId);
    }

    public String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        return "KeepAlive [leaderId=" + leaderId + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private KeepAlive keepAlive;

        public Proxy() {
        }

        Proxy(KeepAlive keepAlive) {
            this.keepAlive = keepAlive;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(keepAlive.getLeaderId());
        }

        @Override
        public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
            keepAlive = new KeepAlive((String) in.readObject());
        }

        private Object readResolve() {
            return keepAlive;
        }
    }
}
