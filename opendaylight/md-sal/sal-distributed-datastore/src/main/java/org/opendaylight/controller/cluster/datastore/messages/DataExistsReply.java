/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DataExistsReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private boolean exists;

    public DataExistsReply() {
    }

    public DataExistsReply(final boolean exists, final short version) {
        super(version);
        this.exists = exists;
    }

    public boolean exists() {
        return exists;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        exists = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeBoolean(exists);
    }

    public static DataExistsReply fromSerializable(final Object serializable) {
        Preconditions.checkArgument(serializable instanceof DataExistsReply);
        return (DataExistsReply)serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof DataExistsReply;
    }
}
