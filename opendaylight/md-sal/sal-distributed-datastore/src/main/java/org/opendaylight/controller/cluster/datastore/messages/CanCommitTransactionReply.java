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

public class CanCommitTransactionReply implements Externalizable {
    private transient boolean canCommit;

    public CanCommitTransactionReply() {
    }

    public CanCommitTransactionReply(boolean canCommit) {
        this.canCommit = canCommit;
    }

    public Boolean getCanCommit() {
        return canCommit;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        canCommit = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(canCommit);
    }
}
