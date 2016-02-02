/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

public class ReadyTransactionReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private String cohortPath;

    public ReadyTransactionReply() {
    }

    public ReadyTransactionReply(String cohortPath) {
        this(cohortPath, DataStoreVersions.CURRENT_VERSION);
    }

    public ReadyTransactionReply(String cohortPath, short version) {
        super(version);
        this.cohortPath = cohortPath;
    }

    public String getCohortPath() {
        return cohortPath;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        cohortPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(cohortPath);
    }

    @Override
    protected Object newLegacySerializedInstance() {
        // no legacy serialized type for this class; return self
        return this;
    }

    public static ReadyTransactionReply fromSerializable(Object serializable) {
        return (ReadyTransactionReply)serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof ReadyTransactionReply;
    }
}
