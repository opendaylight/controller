/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for ReplicatedLogImplEntry.
 *
 * @author Thomas Pantelis
 */
public class ReplicatedLogImplEntryTest {

    @Test
    public void testBackwardsCompatibleDeserializationFromHelium() throws Exception {
        String expPayloadData = "This is a test";
        int expIndex = 1;
        int expTerm = 2;

        try(FileInputStream fis = new FileInputStream("src/test/resources/helium-serialized-ReplicatedLogImplEntry")) {
            ObjectInputStream ois = new ObjectInputStream(fis);

            ReplicatedLogImplEntry entry = (ReplicatedLogImplEntry) ois.readObject();
            ois.close();

            Assert.assertEquals("getIndex", expIndex, entry.getIndex());
            Assert.assertEquals("getTerm", expTerm, entry.getTerm());

            MockRaftActorContext.MockPayload payload = (MockRaftActorContext.MockPayload) entry.getData();
            Assert.assertEquals("data", expPayloadData, payload.toString());
        }
//
//        ReplicatedLogImplEntry entry = new ReplicatedLogImplEntry(expIndex, expTerm,
//                new MockRaftActorContext.MockPayload(expPayloadData));
//        FileOutputStream fos = new FileOutputStream("src/test/resources/helium-serialized-ReplicatedLogImplEntry");
//        ObjectOutputStream oos = new ObjectOutputStream(fos);
//        oos.writeObject(entry);
//        fos.close();
    }
}
