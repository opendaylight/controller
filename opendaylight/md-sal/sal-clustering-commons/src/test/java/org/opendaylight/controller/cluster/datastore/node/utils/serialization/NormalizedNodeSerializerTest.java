package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import static org.junit.Assert.assertEquals;

public class NormalizedNodeSerializerTest {

    @Test
    public void testBasic(){

        long start = System.nanoTime();

        NormalizedNode<?, ?> expectedNode =
            TestModel.createDocumentOne(TestModel.createTestContext());

        NormalizedNodeMessages.Node expected = NormalizedNodeSerializer
            .serialize(expectedNode);

        System.out.println("Serialize Time = " + (System.nanoTime() - start)/1000000);

        System.out.println("Serialized Size = " + expected.getSerializedSize());

        System.out.println(expected.toString());

        start = System.nanoTime();

        NormalizedNode actualNode =
            NormalizedNodeSerializer.deSerialize(expected);

        System.out.println("DeSerialize Time = " + (System.nanoTime() - start)/1000000);

        assertEquals(expectedNode, actualNode);



    }
}
