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

public class RegisterChangeListenerReply implements Externalizable{
    private static final long serialVersionUID = 1L;

    private transient String listenerRegistrationPath;

    public RegisterChangeListenerReply() {
    }

    public RegisterChangeListenerReply(String listenerRegistrationPath) {
        this.listenerRegistrationPath = listenerRegistrationPath;
    }

    public String getListenerRegistrationPath() {
        return listenerRegistrationPath;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        listenerRegistrationPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(listenerRegistrationPath);
    }
}
