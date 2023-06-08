/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;

public interface RaftMessageHandler {

    void writeTo(DataOutput out, RaftRPC message) throws IOException;
    RaftRPC readFrom(DataInput in) throws IOException, ClassNotFoundException;
}