/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.access.ABIVersion;

/**
 * Basic information about how to talk to the backend. ClientActorBehavior uses this information to dispatch requests
 * to the backend.
 *
 * <p>This class is not final so concrete actor behavior implementations may subclass it and track more information
 * about the backend. The {@link #hashCode()} and {@link #equals(Object)} methods are made final to ensure subclasses
 * compare on object identity.
 */
public class BackendInfo {
    private final ABIVersion version;
    private final ActorRef actor;
    private final int maxMessages;
    private final long sessionId;
    private final String name;

    protected BackendInfo(final ActorRef actor, final String name, final long sessionId, final ABIVersion version,
            final int maxMessages) {
        this.version = requireNonNull(version);
        this.actor = requireNonNull(actor);
        this.name = requireNonNull(name);
        checkArgument(maxMessages > 0, "Maximum messages has to be positive, not %s", maxMessages);
        this.maxMessages = maxMessages;
        this.sessionId = sessionId;
    }

    public final ActorRef getActor() {
        return actor;
    }

    public final String getName() {
        return name;
    }

    public final ABIVersion getVersion() {
        return version;
    }

    public final int getMaxMessages() {
        return maxMessages;
    }

    public final long getSessionId() {
        return sessionId;
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
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("actor", actor).add("sessionId", sessionId).add("version", version)
                .add("maxMessages", maxMessages);
    }
}
