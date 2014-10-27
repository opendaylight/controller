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

public class DataExistsReply implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient boolean exists;

    public DataExistsReply() {
    }

    public DataExistsReply(boolean exists) {
        this.exists = exists;
    }

    public boolean exists() {
        return exists;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        exists = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(exists);
    }
}
