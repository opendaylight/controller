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

public class PrimaryNotFound implements Externalizable {
    private static final long serialVersionUID = 1L;

    private String shardName;

    public PrimaryNotFound() {
    }

    public PrimaryNotFound(String shardName){

        Preconditions.checkNotNull(shardName, "shardName should not be null");

        this.shardName = shardName;
    }

    public String getShardName() {
        return shardName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PrimaryNotFound that = (PrimaryNotFound) o;

        if (shardName != null ? !shardName.equals(that.shardName) : that.shardName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return shardName != null ? shardName.hashCode() : 0;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        shardName = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(shardName);
    }
}
