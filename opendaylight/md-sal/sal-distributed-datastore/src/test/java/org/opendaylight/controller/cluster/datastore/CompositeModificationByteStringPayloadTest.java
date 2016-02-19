/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static junit.framework.Assert.assertTrue;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

@Deprecated
public class CompositeModificationByteStringPayloadTest {

    @Test
    public void testSerialization(){
        WriteModification writeModification =
                new WriteModification(TestModel.TEST_PATH, ImmutableNodes
                        .containerNode(TestModel.TEST_QNAME));

        MutableCompositeModification compositeModification =
                new MutableCompositeModification(DataStoreVersions.HELIUM_2_VERSION);

        compositeModification.addModification(writeModification);

        CompositeModificationByteStringPayload compositeModificationByteStringPayload
                = new CompositeModificationByteStringPayload(compositeModification.toSerializable());

        byte[] bytes = SerializationUtils.serialize(compositeModificationByteStringPayload);

        Object deserialize = SerializationUtils.deserialize(bytes);

        assertTrue(deserialize instanceof CompositeModificationByteStringPayload);

    }
}
