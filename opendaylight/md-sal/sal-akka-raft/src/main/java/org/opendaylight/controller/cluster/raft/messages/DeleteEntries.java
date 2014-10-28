/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DeleteEntries implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient int fromIndex;

    public DeleteEntries() {
    }

    public DeleteEntries(int fromIndex) {
        this.fromIndex = fromIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        fromIndex = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(fromIndex);
    }
}