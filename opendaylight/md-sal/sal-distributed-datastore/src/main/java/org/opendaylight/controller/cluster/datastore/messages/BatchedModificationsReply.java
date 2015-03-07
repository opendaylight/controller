/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The reply for the BatchedModifications message.
 *
 * @author Thomas Pantelis
 */
public class BatchedModificationsReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private static final byte COHORT_PATH_NOT_PRESENT = 0;
    private static final byte COHORT_PATH_PRESENT = 1;

    private int numBatched;
    private String cohortPath;

    public BatchedModificationsReply() {
    }

    public BatchedModificationsReply(int numBatched) {
        this.numBatched = numBatched;
    }

    public BatchedModificationsReply(int numBatched, String cohortPath) {
        this.numBatched = numBatched;
        this.cohortPath = cohortPath;
    }

    public int getNumBatched() {
        return numBatched;
    }

    public String getCohortPath() {
        return cohortPath;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        numBatched = in.readInt();

        if(in.readByte() == COHORT_PATH_PRESENT) {
            cohortPath = in.readUTF();
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(numBatched);

        if(cohortPath != null) {
            out.writeByte(COHORT_PATH_PRESENT);
            out.writeUTF(cohortPath);
        } else {
            out.writeByte(COHORT_PATH_NOT_PRESENT);
        }
    }

    @Override
    public Object toSerializable() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BatchedModificationsReply [numBatched=").append(numBatched).append(", cohortPath=")
                .append(cohortPath).append("]");
        return builder.toString();
    }
}
