package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ValueSerializerTest{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSerializeShort(){
        short v1 = 5;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.SHORT_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("5", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.SHORT_TYPE.ordinal(), builder1.getType());
        assertEquals("5", builder.getValue());

    }

    @Test
    public void testSerializeInteger(){
        String hexNumber = "f3";

        Integer expected = Integer.valueOf(hexNumber, 16);


        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), expected);

        assertEquals(ValueType.INT_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("243", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), expected);

        assertEquals(ValueType.INT_TYPE.ordinal(), builder1.getType());
        assertEquals("243", builder1.getValue());


    }


    @Test
    public void testSerializeLong(){
        long v1 = 5;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.LONG_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("5", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.LONG_TYPE.ordinal(), builder1.getType());
        assertEquals("5", builder1.getValue());

    }

    @Test
    public void testSerializeByte(){
        byte v1 = 5;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BYTE_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("5", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BYTE_TYPE.ordinal(), builder1.getType());
        assertEquals("5", builder1.getValue());

    }

    @Test
    public void testSerializeBits(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class),
            ImmutableSet.of("foo", "bar"));

        assertEquals(ValueType.BITS_TYPE.ordinal(), builder.getIntValueType());
        assertTrue( "foo not in bits", builder.getBitsValueList().contains("foo"));
        assertTrue( "bar not in bits", builder.getBitsValueList().contains("bar"));

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class),
            ImmutableSet.of("foo", "bar"));

        assertEquals(ValueType.BITS_TYPE.ordinal(), builder1.getType());
        assertTrue( "foo not in bits", builder1.getBitsValueList().contains("foo"));
        assertTrue( "bar not in bits", builder1.getBitsValueList().contains("bar"));

    }

    @Test
    public void testSerializeWrongTypeOfSet(){
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Expected value type to be Bits but was :");
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class),
            ImmutableSet.of(1, 2));

    }

    @Test
    public void testSerializeEmptyString(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class),"");

        assertEquals(ValueType.STRING_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class),"");

        assertEquals(ValueType.STRING_TYPE.ordinal(), builder1.getType());
        assertEquals("", builder1.getValue());

    }

    @Test
    public void testSerializeString(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class),"foo");

        assertEquals(ValueType.STRING_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("foo", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class),"foo");

        assertEquals(ValueType.STRING_TYPE.ordinal(), builder1.getType());
        assertEquals("foo", builder1.getValue());

    }


    @Test
    public void testSerializeBoolean(){
        boolean v1 = true;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BOOL_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("true", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();
        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BOOL_TYPE.ordinal(), builder1.getType());
        assertEquals("true", builder1.getValue());
    }

    @Test
    public void testSerializeQName(){
        QName v1 = TestModel.TEST_QNAME;
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.QNAME_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.QNAME_TYPE.ordinal(), builder1.getType());
        assertEquals("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test", builder1.getValue());

    }

    @Test
    public void testSerializeYangIdentifier(){
        YangInstanceIdentifier v1 = TestModel.TEST_PATH;

        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        QNameSerializationContext mockContext = mock(QNameSerializationContext.class);
        ValueSerializer.serialize(builder, mockContext, v1);

        assertEquals(ValueType.YANG_IDENTIFIER_TYPE.ordinal(), builder.getIntValueType());
        NormalizedNodeMessages.InstanceIdentifier serializedYangInstanceIdentifier =
            builder.getInstanceIdentifierValue();

        assertEquals(1, serializedYangInstanceIdentifier.getArgumentsCount());
        Mockito.verify(mockContext).addLocalName(TestModel.TEST_QNAME.getLocalName());
        Mockito.verify(mockContext).addNamespace(TestModel.TEST_QNAME.getNamespace());

        NormalizedNodeMessages.PathArgumentAttribute.Builder argumentBuilder
                = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        mockContext = mock(QNameSerializationContext.class);

        ValueSerializer.serialize(argumentBuilder, mockContext, v1);

        serializedYangInstanceIdentifier =
                argumentBuilder.getInstanceIdentifierValue();

        assertEquals(1, serializedYangInstanceIdentifier.getArgumentsCount());
        Mockito.verify(mockContext).addLocalName(TestModel.TEST_QNAME.getLocalName());
        Mockito.verify(mockContext).addNamespace(TestModel.TEST_QNAME.getNamespace());

    }

    @Test
    public void testSerializeBigInteger(){
        BigInteger v1 = new BigInteger("1000000000000000000000000");
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BIG_INTEGER_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("1000000000000000000000000", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BIG_INTEGER_TYPE.ordinal(), builder1.getType());
        assertEquals("1000000000000000000000000", builder1.getValue());

    }

    @Test
    public void testSerializeBigDecimal(){
        BigDecimal v1 = new BigDecimal("1000000000000000000000000.51616");
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BIG_DECIMAL_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("1000000000000000000000000.51616", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();
        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class), v1);

        assertEquals(ValueType.BIG_DECIMAL_TYPE.ordinal(), builder1.getType());
        assertEquals("1000000000000000000000000.51616", builder1.getValue());

    }

    @Test
    public void testSerializeBinary(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        byte[] bytes = new byte[] {1,2,3,4};
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class),bytes);

        assertEquals(ValueType.BINARY_TYPE.ordinal(), builder.getIntValueType());
        assertEquals(ByteString.copyFrom(bytes), builder.getBytesValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class),bytes);

        assertEquals(ValueType.BINARY_TYPE.ordinal(), builder1.getType());
        assertEquals(ByteString.copyFrom(bytes), builder1.getBytesValue());

    }

    @Test
    public void testSerializeNull(){
        NormalizedNodeMessages.Node.Builder builder = NormalizedNodeMessages.Node.newBuilder();
        Object none = null;
        ValueSerializer.serialize(builder, mock(QNameSerializationContext.class),none);

        assertEquals(ValueType.NULL_TYPE.ordinal(), builder.getIntValueType());
        assertEquals("", builder.getValue());

        NormalizedNodeMessages.PathArgumentAttribute.Builder builder1 = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        ValueSerializer.serialize(builder1, mock(QNameSerializationContext.class),none);

        assertEquals(ValueType.NULL_TYPE.ordinal(), builder1.getType());
        assertEquals("", builder.getValue());

    }


    @Test
    public void testDeSerializeShort(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.SHORT_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof Short);
        assertEquals(25, ((Short) o).shortValue());
    }

    @Test
    public void testDeSerializeByte(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.BYTE_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof Byte);
        assertEquals(25, ((Byte) o).byteValue());

    }

    @Test
    public void testDeSerializeInteger(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.INT_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof Integer);
        assertEquals(25, ((Integer) o).intValue());

    }

    @Test
    public void testDeSerializeLong(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.LONG_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof Long);
        assertEquals(25, ((Long) o).longValue());

    }

    @Test
    public void testDeSerializeBoolean(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.BOOL_TYPE.ordinal());
        nodeBuilder.setValue("false");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof Boolean);
        assertEquals(false, ((Boolean) o).booleanValue());

    }

    @Test
    public void testDeSerializeQName(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.QNAME_TYPE.ordinal());
        nodeBuilder.setValue(TestModel.TEST_QNAME.toString());

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof QName);
        assertEquals(TestModel.TEST_QNAME, o);

    }

    @Test
    public void testDeSerializeBits(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.BITS_TYPE.ordinal());
        nodeBuilder.addAllBitsValue(ImmutableList.of("foo", "bar"));

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof Set);
        assertTrue(((Set<?>)o).contains("foo"));
        assertTrue(((Set<?>) o).contains("bar"));

        NormalizedNodeMessages.PathArgumentAttribute.Builder argumentBuilder
                = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        argumentBuilder.setType(ValueType.BITS_TYPE.ordinal());
        argumentBuilder.addAllBitsValue(ImmutableList.of("foo", "bar"));

        o = ValueSerializer
                .deSerialize(mock(QNameDeSerializationContext.class),
                        argumentBuilder.build());

        assertTrue(o instanceof Set);
        assertTrue(((Set<?>)o).contains("foo"));
        assertTrue(((Set<?>) o).contains("bar"));

    }

    @Test
    public void testDeSerializeYangIdentifier(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        NormalizedNodeMessages.InstanceIdentifier.Builder idBuilder = NormalizedNodeMessages.InstanceIdentifier.newBuilder();
        NormalizedNodeMessages.PathArgument.Builder pathBuilder = NormalizedNodeMessages.PathArgument.newBuilder();

        pathBuilder.setIntType(PathArgumentType.NODE_IDENTIFIER.ordinal());

        idBuilder.addArguments(pathBuilder);

        nodeBuilder.setIntValueType(ValueType.YANG_IDENTIFIER_TYPE.ordinal());
        nodeBuilder.setInstanceIdentifierValue(idBuilder);

        QNameDeSerializationContext mockContext = mock(QNameDeSerializationContext.class);
        Mockito.doReturn(TestModel.TEST_QNAME.getNamespace().toString()).when(mockContext).
                getNamespace(Mockito.anyInt());
        Mockito.doReturn(TestModel.TEST_QNAME.getLocalName()).when(mockContext).
                getLocalName(Mockito.anyInt());
        Mockito.doReturn(TestModel.TEST_QNAME.getFormattedRevision()).when(mockContext).
                getRevision(Mockito.anyInt());

        Object o = ValueSerializer.deSerialize(mockContext, nodeBuilder.build());

        assertTrue(o instanceof YangInstanceIdentifier);
        assertEquals(TestModel.TEST_PATH, o);

        NormalizedNodeMessages.PathArgumentAttribute.Builder argumentBuilder =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        argumentBuilder.setType(ValueType.YANG_IDENTIFIER_TYPE.ordinal());
        argumentBuilder.setInstanceIdentifierValue(idBuilder);

        o = ValueSerializer.deSerialize(mockContext, argumentBuilder.build());

        assertTrue(o instanceof YangInstanceIdentifier);
        assertEquals(TestModel.TEST_PATH, o);
    }

    @Test
    public void testDeSerializeString(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.STRING_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer.deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof String);
        assertEquals("25", o);

    }

    @Test
    public void testDeSerializeBigInteger(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.BIG_INTEGER_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof BigInteger);
        assertEquals(new BigInteger("25"), o);

    }

    @Test
    public void testDeSerializeBigDecimal(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.BIG_DECIMAL_TYPE.ordinal());
        nodeBuilder.setValue("25");

        Object o = ValueSerializer
            .deSerialize(mock(QNameDeSerializationContext.class),
                nodeBuilder.build());

        assertTrue(o instanceof BigDecimal);
        assertEquals(new BigDecimal("25"), o);

    }


    @Test
    public void testDeSerializeBinaryType(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.BINARY_TYPE.ordinal());
        byte[] bytes = new byte[] {1,2,3,4};
        nodeBuilder.setBytesValue(ByteString.copyFrom(bytes));

        Object o = ValueSerializer.deSerialize(mock(QNameDeSerializationContext.class),nodeBuilder.build());

        assertTrue("not a byte array", o instanceof byte[]);
        assertTrue("bytes value does not match" , Arrays.equals(bytes, (byte[]) o));

        NormalizedNodeMessages.PathArgumentAttribute.Builder argumentBuilder =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();
        argumentBuilder.setType(ValueType.BINARY_TYPE.ordinal());
        argumentBuilder.setBytesValue(ByteString.copyFrom(bytes));

        o = ValueSerializer.deSerialize(mock(QNameDeSerializationContext.class), argumentBuilder.build());

        assertTrue("not a byte array", o instanceof byte[]);
        assertTrue("bytes value does not match" ,Arrays.equals(bytes, (byte[]) o));


    }

    @Test
    public void testDeSerializeNullType(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        nodeBuilder.setIntValueType(ValueType.NULL_TYPE.ordinal());
        nodeBuilder.setValue("");

        Object o = ValueSerializer
                .deSerialize(mock(QNameDeSerializationContext.class),
                        nodeBuilder.build());

        assertEquals(null, o);

        NormalizedNodeMessages.PathArgumentAttribute.Builder argumentBuilder
                = NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        argumentBuilder.setType(ValueType.NULL_TYPE.ordinal());
        argumentBuilder.setValue("");

        o = ValueSerializer
                .deSerialize(mock(QNameDeSerializationContext.class),
                        argumentBuilder.build());

        assertEquals(null, o);

    }


}
