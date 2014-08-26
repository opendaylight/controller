package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import junit.framework.TestCase;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeSerializerTest extends TestCase {

    @Test
    public void testBasic(){

        long start = System.nanoTime();
        NormalizedNodeMessages.Node expected = NormalizedNodeSerializer
            .serialize(
                TestModel.createDocumentOne(TestModel.createTestContext()));

        System.out.println("Serialize Time = " + (System.nanoTime() - start)/1000000);

        System.out.println("Serialized Size = " + expected.getSerializedSize());

        System.out.println(expected.toString());

        start = System.nanoTime();

        NormalizedNode output =
            NormalizedNodeSerializer.deSerialize(expected);

        System.out.println("DeSerialize Time = " + (System.nanoTime() - start)/1000000);

    }
}
