/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.ABIVersion;

/**
 * Basic information about how to talk to the backend. ClientActorBehavior uses this information to dispatch requests
 * to the backend.
 *
 * This class is not final so concrete actor behavior implementations may subclass it and track more information about
 * the backend. The {@link #hashCode()} and {@link #equals(Object)} methods are made final to ensure subclasses compare
 * on identity.
 *
 * @author Robert Varga
 */
public class BackendInfo {
    private final ABIVersion version;
    private final ActorRef actor;

    protected BackendInfo(final ActorRef actor, final ABIVersion version) {
        this.version = Preconditions.checkNotNull(version);
        this.actor = Preconditions.checkNotNull(actor);
    }

    public final ActorRef getActor() {
        return actor;
    }

    public final ABIVersion getVersion() {
        return version;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("actor", actor).add("version", version);
    }
}
