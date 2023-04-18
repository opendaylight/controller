/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message sent to leader to transfer leadership to a particular follower.
 */
public final class RequestLeadership implements Serializable, SerializableMessage {
    private static final long serialVersionUID = 1L;

    private final String requestedFollowerId;
    private final ActorRef replyTo;

    public RequestLeadership(final String requestedFollowerId, final ActorRef replyTo) {
        this.requestedFollowerId = requireNonNull(requestedFollowerId);
        this.replyTo = requireNonNull(replyTo);
    }

    public String getRequestedFollowerId() {
        return requestedFollowerId;
    }

    public ActorRef getReplyTo() {
        return replyTo;
    }

    @Override
    public String toString() {
        return "RequestLeadership [requestedFollowerId=" + requestedFollowerId + ", replyTo=" + replyTo + "]";
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.writeUTF(requestedFollowerId);

        // TODO: possible better way to serialize
        final byte[] actorRef = SerializationUtils.serialize(replyTo);
        out.write(actorRef.length);
        out.write(actorRef);
    }

    public static @NonNull RequestLeadership readFrom(final DataInput in) throws IOException {
        final String followerId = in.readUTF();

        final int size = in.readInt();
        final byte[] data = new byte[size];
        in.readFully(data);
        final var actorRef = (ActorRef) SerializationUtils.deserialize(data);

        return new RequestLeadership(followerId, actorRef);
    }
}
