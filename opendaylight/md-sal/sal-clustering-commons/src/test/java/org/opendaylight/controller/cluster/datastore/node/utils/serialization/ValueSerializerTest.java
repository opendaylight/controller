package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

public class ValueSerializerTest{

    @Test
    public void testSerializeShort(){
        short v1 = 5;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class), v1);

        assertEquals(ValueType.SHORT_TYPE, (Integer) builder.getIntValueType());
        assertEquals("5", builder.getValue());

    }

    @Test
    public void testSerializeInteger(){
        String hexNumber = "f3";

        Integer expected = Integer.valueOf(hexNumber, 16);


        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class), expected);

        assertEquals(ValueType.INT_TYPE, (Integer) builder.getIntValueType());
        assertEquals("243", builder.getValue());

    }


    @Test
    public void testSerializeLong(){
        long v1 = 5;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class), v1);

        assertEquals(ValueType.LONG_TYPE, (Integer) builder.getIntValueType());
        assertEquals("5", builder.getValue());

    }

    @Test
    public void testSerializeByte(){
        byte v1 = 5;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class), v1);

        assertEquals(ValueType.BYTE_TYPE, (Integer) builder.getIntValueType());
        assertEquals("5", builder.getValue());

    }

    @Test
    public void testSerializeBits(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class),
            ImmutableSet.of("foo", "bar"));

        assertEquals(ValueType.BITS_TYPE, (Integer) builder.getIntValueType());
        assertEquals("[foo, bar]", builder.getValue());


    }

    @Test
    public void testSerializeEmptyString(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class),"");

        assertEquals(ValueType.STRING_TYPE, (Integer) builder.getIntValueType());
        assertEquals("", builder.getValue());

    }

    @Test
    public void testSerializeString(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, any(NormalizedNodeSerializationContext.class),"foo");

        assertEquals(ValueType.STRING_TYPE, (Integer) builder.getIntValueType());
        assertEquals("foo", builder.getValue());
    }


}
