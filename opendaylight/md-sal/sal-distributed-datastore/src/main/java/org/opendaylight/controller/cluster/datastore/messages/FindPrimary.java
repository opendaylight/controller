/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import com.google.common.base.Preconditions;

/**
 * The FindPrimary message is used to locate the primary of any given shard
 *
 */
public class FindPrimary implements Externalizable {
    private static final long serialVersionUID = 1L;

    private String shardName;
    private boolean waitUntilInitialized;

    public FindPrimary() {
    }

    public FindPrimary(String shardName, boolean waitUntilInitialized){

        Preconditions.checkNotNull(shardName, "shardName should not be null");

        this.shardName = shardName;
        this.waitUntilInitialized = waitUntilInitialized;
    }

    public String getShardName() {
        return shardName;
    }

    public boolean isWaitUntilInitialized() {
        return waitUntilInitialized;
    }

    public static FindPrimary fromSerializable(Object message){
        return (FindPrimary) message;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        shardName = in.readUTF();
        waitUntilInitialized = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(shardName);
        out.writeBoolean(waitUntilInitialized);
    }
}
