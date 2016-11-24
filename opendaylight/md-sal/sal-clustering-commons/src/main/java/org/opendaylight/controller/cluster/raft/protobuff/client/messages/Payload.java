/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.protobuff.client.messages;

/**
 * An instance of a Payload class is meant to be used as the Payload for
 * AppendEntries.
 *
 * <p>
 * When an actor which is derived from RaftActor attempts to persistData it
 * must pass an instance of the Payload class. Similarly when state needs to
 * be applied to the derived RaftActor it will be passed an instance of the
 * Payload class.
 */
public abstract class Payload {
    public abstract int size();
}
