/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.protobuff.client.messages;


import com.google.protobuf.GeneratedMessage;
import java.util.Map;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

/**
 * An instance of a Payload class is meant to be used as the Payload for
 * AppendEntries.
 * <p>
 *
 * When an actor which is derived from RaftActor attempts to persistData it
 * must pass an instance of the Payload class. Similarly when state needs to
 * be applied to the derived RaftActor it will be passed an instance of the
 * Payload class.
 * <p>
 *
 * To define your own payload do the following,
 * <ol>
 *     <li>Create your own protocol buffer message which extends the AppendEntries Payload</li>
 *     <li>Extend this Payload class</li>
 *     <li>Implement encode</li>
 *     <li>Implement decode</li>
 * </ol>
 *
 * Your own protocol buffer message can be create like so, <br/>
 * <pre>
 * {@code
 *
 * import "AppendEntriesMessages.proto";
 *
 * package org.opendaylight.controller.cluster.raft;
 *
 * option java_package = "org.opendaylight.controller.cluster.raft.protobuff.messages";
 * option java_outer_classname = "MockPayloadMessages";
 *
 * extend AppendEntries.ReplicatedLogEntry.Payload {
 *      optional string value = 2;
 * }
 * }
 * </pre>
 *
 */
public abstract class Payload {

    public String getClientPayloadClassName() {
        return this.getClass().getName();
    }

    /**
     * Encode the payload data as a protocol buffer extension.
     * <p>
     * TODO: Add more meat in here
     * @param <T>
     * @return Map of <GeneratedMessage.GeneratedExtension, T>
     */
    @Deprecated
    public abstract <T extends Object> Map<GeneratedMessage.GeneratedExtension, T> encode();

    /**
     * Decode the protocol buffer payload into a specific Payload as defined
     * by the class extending RaftActor
     *
     * @param payload The payload in protocol buffer format
     * @return
     */
    @Deprecated
    public abstract Payload decode(
        AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payload);

    public abstract int size();
}
