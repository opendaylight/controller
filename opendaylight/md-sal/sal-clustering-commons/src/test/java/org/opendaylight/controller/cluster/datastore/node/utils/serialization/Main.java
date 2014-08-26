/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        while(true) {
            Thread.sleep(1000);

            long start = System.nanoTime();
            NormalizedNodeMessages.Node expected = NormalizedNodeSerializer
                .serialize(
                    TestModel.createDocumentOne(TestModel.createTestContext()));

            System.out.println(
                "Serialize Time = " + (System.nanoTime() - start) / 1000000);

            System.out
                .println("Serialized Size = " + expected.getSerializedSize());

//            System.out.println(expected.toString());

            start = System.nanoTime();

            NormalizedNode output =
                NormalizedNodeSerializer.deSerialize(expected);

            System.out.println(
                "DeSerialize Time = " + (System.nanoTime() - start) / 1000000);
        }

    }
}
