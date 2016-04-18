/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

@Beta
public final class CreateLocalHistorySuccess extends AbstractLocalHistorySuccess {
    private static final class Proxy extends LocalHistorySuccessProxy {
        private static final long serialVersionUID = 1L;
        private ActorRef backendHistoryRef;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier identifier, final ActorRef backendHistoryRef) {
            super(identifier);
            this.backendHistoryRef = Preconditions.checkNotNull(backendHistoryRef);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeObject(backendHistoryRef);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            backendHistoryRef = (ActorRef) in.readObject();
        }

        @Override
        protected CreateLocalHistorySuccess readResolve() {
            return new CreateLocalHistorySuccess(getIdentifier(), backendHistoryRef);
        }
    }

    private static final long serialVersionUID = 1L;
    private final ActorRef backendHistoryRef;

    public CreateLocalHistorySuccess(final LocalHistoryIdentifier historyId, final ActorRef backendHistoryRef) {
        super(historyId);
        this.backendHistoryRef = Preconditions.checkNotNull(backendHistoryRef);
    }

    public ActorRef getBackendHistoryRef() {
        return backendHistoryRef;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("actor", backendHistoryRef);
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), backendHistoryRef);
    }
}
