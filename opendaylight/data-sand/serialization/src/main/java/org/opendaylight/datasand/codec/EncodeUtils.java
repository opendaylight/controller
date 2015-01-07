package org.opendaylight.datasand.codec;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.datasand.codec.observers.IAugmetationObserver;

public class EncodeUtils {

    private static final byte NULL_VALUE_1 = (byte) 'N';
    private static final byte NULL_VALUE_2 = (byte) 'L';
    private static Map<Class<?>,ISerializer> registeredSerializers = new ConcurrentHashMap<Class<?>,ISerializer>();
    private static Map<Integer,Class<?>>  registeredSerializersID = new ConcurrentHashMap<Integer,Class<?>>();
    private static Map<Class<?>,Integer> registeredSerializersClassCode = new ConcurrentHashMap<Class<?>,Integer>();

    public static void registerSerializer(Class<?> cls,ISerializer serializer,int classID){
        registeredSerializers.put(cls, serializer);
        registeredSerializersID.put(classID,cls);
        registeredSerializersClassCode.put(cls, classID);
    }

    public static Integer getClassCodeByClass(Class<?> cls){
        for(Map.Entry<Integer, Class<?>> entry:registeredSerializersID.entrySet()){
            if(entry.getValue().equals(cls))
                return entry.getKey();
        }
        return null;
    }

    public static ISerializer getRegisteredSerializer(Class<?> cls){
        ISerializer serializer = registeredSerializers.get(cls);
        if(serializer!=null)
            return serializer;
        return null;
    }

    public static ISerializer getSerializer(Class<?> cls,TypeDescriptorsContainer container) {
        ISerializer serializer = registeredSerializers.get(cls);
        if(serializer!=null)
            return serializer;
        if(container!=null){
            TypeDescriptor type = container.getTypeDescriptorByClass(cls);
            if (type != null) {
                return type.getSerializer();
            }
        }
        return null;
    }

    public static ISerializer getSerializer(int typeCode,TypeDescriptorsContainer container) {
        Class<?> cls = registeredSerializersID.get(typeCode);
        if(cls!=null){
            return registeredSerializers.get(cls);
        }
        if(container!=null){
            TypeDescriptor type = container.getTypeDescriptorByCode(
                    typeCode);
            if (type != null) {
                return type.getSerializer();
            }
        }
        return null;
    }

    public static final void encodeInt16(int value, byte[] byteArray,int location) {
        byteArray[location + 1] = (byte) (value >> 8);
        byteArray[location] = (byte) (value);
    }

    public static final void encodeInt16(int value, EncodeDataContainer ba) {
        ba.adjustSize(2);
        encodeInt16(value, ba.getBytes(), ba.getLocation());
        ba.advance(2);
    }

    public static final int decodeInt16(byte[] byteArray, int location) {
        int value = 0;
        if (byteArray[location] > 0) {
            value += ((int) byteArray[location] & 0xFFL);
        } else {
            value += ((int) (256 + byteArray[location]) & 0xFFL);
        }

        if (byteArray[location + 1] > 0) {
            value += ((int) byteArray[location + 1] & 0xFFL) << (8 * (1));
        } else {
            value += ((int) (256 + byteArray[location + 1]) & 0xFFL) << (8 * (1));
        }
        return value;
    }

    public static final void encodeShort(short value, byte[] byteArray,int location) {
        byteArray[location + 1] = (byte) (value >> 8);
        byteArray[location] = (byte) (value);
    }

    public static final void encodeShort(Short value, EncodeDataContainer ba) {
        if(value==null) value = 0;
        ba.adjustSize(2);
        encodeShort(value, ba.getBytes(), ba.getLocation());
        ba.advance(2);
    }

    public static final short decodeShort(byte[] byteArray, int location) {
        short value = 0;
        if (byteArray[location] > 0) {
            value += ((int) byteArray[location] & 0xFFL);
        } else {
            value += ((int) (256 + byteArray[location]) & 0xFFL);
        }

        if (byteArray[location + 1] > 0) {
            value += ((int) byteArray[location + 1] & 0xFFL) << (8 * (1));
        } else {
            value += ((int) (256 + byteArray[location + 1]) & 0xFFL) << (8 * (1));
        }
        return value;
    }

    public static final short decodeShort(EncodeDataContainer ba) {
        short value = decodeShort(ba.getBytes(), ba.getLocation());
        ba.advance(2);
        return value;
    }

    public static final int decodeInt16(EncodeDataContainer ba) {
        int value = decodeInt16(ba.getBytes(), ba.getLocation());
        ba.advance(2);
        return value;
    }

    public static final void encodeInt32(int value, byte byteArray[],
            int location) {
        byteArray[location] = (byte) ((value >> 24) & 0xff);
        byteArray[location + 1] = (byte) ((value >> 16) & 0xff);
        byteArray[location + 2] = (byte) ((value >> 8) & 0xff);
        byteArray[location + 3] = (byte) ((value >> 0) & 0xff);
    }

    public static final void encodeInt32(Integer value, EncodeDataContainer ba) {
        if(value==null) value = 0;
        ba.adjustSize(4);
        encodeInt32(value, ba.getBytes(), ba.getLocation());
        ba.advance(4);
    }

    public static final int decodeInt32(byte[] byteArray, int location) {
        int value = (int) (0xff & byteArray[location]) << 24
                | (int) (0xff & byteArray[location + 1]) << 16
                | (int) (0xff & byteArray[location + 2]) << 8
                | (int) (0xff & byteArray[location + 3]) << 0;
        return value;
    }

    public static final int decodeUInt32(byte[] byteArray, int location) {
        int value = 0;
        if (byteArray[location] > 0) {
            value += ((int) byteArray[location] & 0xFFL);
        } else {
            value += ((int) (256 + byteArray[location]) & 0xFFL);
        }

        if (byteArray[location + 1] > 0) {
            value += ((int) byteArray[location + 1] & 0xFFL) << (8 * (1));
        } else {
            value += ((int) (256 + byteArray[location + 1]) & 0xFFL) << (8 * (1));
        }

        if (byteArray[location + 2] > 0) {
            value += ((int) byteArray[location + 2] & 0xFFL) << (8 * (2));
        } else {
            value += ((int) (256 + byteArray[location + 2]) & 0xFFL) << (8 * (2));
        }

        if (byteArray[location + 3] > 0) {
            value += ((int) byteArray[location + 3] & 0xFFL) << (8 * (3));
        } else {
            value += ((int) (256 + byteArray[location + 3]) & 0xFFL) << (8 * (3));
        }
        return value;
    }

    public static final int decodeInt32(EncodeDataContainer ba) {
        int value = decodeInt32(ba.getBytes(), ba.getLocation());
        ba.advance(4);
        return value;
    }

    public static final void encodeInt64(long value, byte[] byteArray,int location) {
        byteArray[location] = (byte) ((value >> 56) & 0xff);
        byteArray[location + 1] = (byte) ((value >> 48) & 0xff);
        byteArray[location + 2] = (byte) ((value >> 40) & 0xff);
        byteArray[location + 3] = (byte) ((value >> 32) & 0xff);
        byteArray[location + 4] = (byte) ((value >> 24) & 0xff);
        byteArray[location + 5] = (byte) ((value >> 16) & 0xff);
        byteArray[location + 6] = (byte) ((value >> 8) & 0xff);
        byteArray[location + 7] = (byte) ((value >> 0) & 0xff);
    }

    public static final void encodeInt64(Long value, EncodeDataContainer ba) {
        if(value==null) value = 0L;
        ba.adjustSize(8);
        encodeInt64(value, ba.getBytes(), ba.getLocation());
        ba.advance(8);
    }

    public static final void encodeBigDecimal(BigDecimal value, EncodeDataContainer ba) {
        if(value==null){
            encodeNULL(ba);
            return;
        }
        encodeString(value.toString(), ba);
        /* The method Double.doubleToLongBits is EXTREMELY slow! Need
         * To find other solution to that
         *
        long longValue = Double.doubleToLongBits(value.doubleValue());
        ba.adjustSize(8);
        encodeInt64(longValue, ba.getBytes(), ba.getLocation());
        ba.advance(8);
        */
    }

    public static final BigDecimal decodeBigDecimal(EncodeDataContainer ba){
        if(isNULL(ba))
            return null;
        return new BigDecimal(decodeString(ba));
        /*
        long longValue = decodeInt64(ba);
        return new BigDecimal(Double.longBitsToDouble(longValue));
        */
    }

    public static final long decodeInt64(byte[] byteArray, int location) {
        long value = (long) (0xff & byteArray[location]) << 56
                | (long) (0xff & byteArray[location + 1]) << 48
                | (long) (0xff & byteArray[location + 2]) << 40
                | (long) (0xff & byteArray[location + 3]) << 32
                | (long) (0xff & byteArray[location + 4]) << 24
                | (long) (0xff & byteArray[location + 5]) << 16
                | (long) (0xff & byteArray[location + 6]) << 8
                | (long) (0xff & byteArray[location + 7]) << 0;
        return value;
    }

    public static final long decodeUInt64(byte[] byteArray, int location) {
        long value = 0;
        if (byteArray[location] > 0) {
            value += ((long) byteArray[location] & 0xFFL);
        } else {
            value += ((long) (256 + byteArray[location]) & 0xFFL);
        }

        if (byteArray[location + 1] > 0) {
            value += ((long) byteArray[location + 1] & 0xFFL) << (8 * (1));
        } else {
            value += ((long) (256 + byteArray[location + 1]) & 0xFFL) << (8 * (1));
        }

        if (byteArray[location + 2] > 0) {
            value += ((long) byteArray[location + 2] & 0xFFL) << (8 * (2));
        } else {
            value += ((long) (256 + byteArray[location + 2]) & 0xFFL) << (8 * (2));
        }

        if (byteArray[location + 3] > 0) {
            value += ((long) byteArray[location + 3] & 0xFFL) << (8 * (3));
        } else {
            value += ((long) (256 + byteArray[location + 3]) & 0xFFL) << (8 * (3));
        }

        if (byteArray[location + 4] > 0) {
            value += ((long) byteArray[location + 4] & 0xFFL) << (8 * (4));
        } else {
            value += ((long) (256 + byteArray[location + 4]) & 0xFFL) << (8 * (4));
        }

        if (byteArray[location + 5] > 0) {
            value += ((long) byteArray[location + 5] & 0xFFL) << (8 * (5));
        } else {
            value += ((long) (256 + byteArray[location + 5]) & 0xFFL) << (8 * (5));
        }

        if (byteArray[location + 6] > 0) {
            value += ((long) byteArray[location + 6] & 0xFFL) << (8 * (6));
        } else {
            value += ((long) (256 + byteArray[location + 6]) & 0xFFL) << (8 * (6));
        }

        if (byteArray[location + 7] > 0) {
            value += ((long) byteArray[location + 7] & 0xFFL) << (8 * (7));
        } else {
            value += ((long) (256 + byteArray[location + 7]) & 0xFFL) << (8 * (7));
        }

        return value;
    }

    public static final long decodeInt64(EncodeDataContainer ba) {
        long value = decodeInt64(ba.getBytes(), ba.getLocation());
        ba.advance(8);
        return value;
    }

    public static final void encodeString(String value, byte[] byteArray,
            int location) {
        byte bytes[] = value.getBytes();
        encodeInt16(bytes.length, byteArray, location);
        System.arraycopy(bytes, 0, byteArray, location + 2, bytes.length);
    }

    public static final void encodeString(String value, EncodeDataContainer ba) {
        if (value == null) {
            encodeNULL(ba);
        } else {
            byte bytes[] = value.getBytes();
            ba.adjustSize(bytes.length + 2);
            encodeInt16(bytes.length, ba.getBytes(), ba.getLocation());
            System.arraycopy(bytes, 0, ba.getBytes(), ba.getLocation() + 2,
                    bytes.length);
            ba.advance(bytes.length + 2);
        }
    }

    public static final String decodeString(byte[] byteArray, int location) {
        int size = decodeInt16(byteArray, location);
        return new String(byteArray, location + 2, size);
    }

    public static final String decodeString(EncodeDataContainer ba) {
        if (isNULL(ba)) {
            return null;
        }
        int size = decodeInt16(ba.getBytes(), ba.getLocation());
        String result = new String(ba.getBytes(), ba.getLocation() + 2, size);
        ba.advance(size + 2);
        return result;
    }

    public static final void encodeByteArray(byte[] value, byte byteArray[],int location) {
        encodeInt32(value.length, byteArray, location);
        System.arraycopy(value, 0, byteArray, location + 4, value.length);
    }

    public static final void encodeByteArray(byte[] value, EncodeDataContainer ba) {
        if (value == null) {
            encodeNULL(ba);
        } else {
            ba.adjustSize(value.length + 4);
            encodeByteArray(value, ba.getBytes(), ba.getLocation());
            ba.advance(value.length + 4);
        }
    }

    public static final byte[] decodeByteArray(byte[] byteArray, int location) {
        int size = decodeInt32(byteArray, location);
        byte array[] = new byte[size];
        System.arraycopy(byteArray, location + 4, array, 0, array.length);
        return array;
    }

    public static final byte[] decodeByteArray(EncodeDataContainer ba) {
        if (isNULL(ba)) {
            return null;
        }
        byte[] array = decodeByteArray(ba.getBytes(), ba.getLocation());
        ba.advance(array.length + 4);
        return array;
    }

    public static final void encodeBoolean(boolean value, byte[] byteArray,int location) {
        if (value)
            byteArray[location] = 1;
        else
            byteArray[location] = 0;
    }

    public static final void encodeBoolean(boolean value, EncodeDataContainer ba) {
        ba.adjustSize(1);
        encodeBoolean(value, ba.getBytes(), ba.getLocation());
        ba.advance(1);
    }

    public static final boolean decodeBoolean(byte[] byteArray, int location) {
        if (byteArray[location] == 1)
            return true;
        else
            return false;
    }

    public static final boolean decodeBoolean(EncodeDataContainer ba) {
        boolean result = decodeBoolean(ba.getBytes(), ba.getLocation());
        ba.advance(1);
        return result;
    }

    public static final void encodeByte(Byte value, EncodeDataContainer ba) {
        if(value==null) value = (byte)0;
        ba.adjustSize(1);
        ba.getBytes()[ba.getLocation()] = value;
        ba.advance(1);
    }

    public static final byte decodeByte(EncodeDataContainer ba){
       ba.advance(1);
       return ba.getBytes()[ba.getLocation()-1];
    }

    public static final void encodeAugmentations(Object value, EncodeDataContainer ba) {
        IAugmetationObserver ao = ba.getTypeDescriptorContainer().getAugmentationObserver();
        if(ao!=null){
            ao.encodeAugmentations(value, ba);
        }
    }

    public static final void decodeAugmentations(Object builder, EncodeDataContainer ba,Class<?> augmentedClass) {
        IAugmetationObserver ao = ba.getTypeDescriptorContainer().getAugmentationObserver();
        if(ao!=null){
            ao.decodeAugmentations(builder, ba, augmentedClass);
        }
    }

    public static final void encodeAndAddObject(Object value, EncodeDataContainer ba,Class<?> objectType) {
        if (value == null) {
            encodeNULL(ba);
        } else {
            TypeDescriptor tbl = ba.getTypeDescriptorContainer().getTypeDescriptorByClass(objectType);
            int classCode = tbl.getClassCode();
            encodeInt16(classCode,ba);
            EncodeDataContainer subBA = new EncodeDataContainer(1024,ba.getTypeDescriptorContainer());
            ISerializer serializer = getSerializer(objectType,ba.getTypeDescriptorContainer());
            if (serializer != null) {
                encodeInt16(classCode, subBA);
                serializer.encode(value, subBA);
                ba.addSubElementData(classCode, subBA,tbl.getMD5IDForObject(value));
            } else {
                System.err.println("Can't find a serializer for " + objectType);
            }
        }
    }

    public static final void encodeObject(Object value, EncodeDataContainer ba,Class<?> objectType) {
        if(value instanceof String){
            encodeInt16(FrameworkClassCodes.CODE_String,ba);
            encodeString((String)value, ba);
            return;
        }else
        if(value instanceof Integer){
            encodeInt16(FrameworkClassCodes.CODE_Integer,ba);
            encodeInt32((Integer)value,ba);
            return;
        }else
        if(value instanceof Long){
            encodeInt16(FrameworkClassCodes.CODE_Long,ba);
            encodeInt64((Long)value,ba);
            return;
        }else
        if(value instanceof List){
            encodeInt16(FrameworkClassCodes.CODE_List, ba);
            encodeList((List)value, ba);
            return;
        }

        if (value == null) {
            encodeNULL(ba);
            return;
        }

        if(objectType==null)
            objectType = value.getClass();

        ISerializer serializer = getSerializer(objectType,ba.getTypeDescriptorContainer());
        if (serializer != null) {
            Integer classCode = registeredSerializersClassCode.get(objectType);
            if(classCode==null){
                classCode = ba.getTypeDescriptorContainer().getTypeDescriptorByClass(objectType).getClassCode();
            }
            if(classCode==null){
                System.err.println("Unable To find a code for this class:"+objectType.getName());
                return;
            }
            encodeInt16(classCode, ba);
            serializer.encode(value, ba);
        } else {
            System.err.println("Can't find a serializer for " + objectType);
        }
    }

    public static final Object decodeObject(EncodeDataContainer ba) {
        if (isNULL(ba)) {
            return null;
        }
        int classCode = decodeInt16(ba);
        if(classCode==FrameworkClassCodes.CODE_String){
            return decodeString(ba);
        }else
        if(classCode==FrameworkClassCodes.CODE_Integer){
            return decodeInt32(ba);
        }else
        if(classCode==FrameworkClassCodes.CODE_Long){
            return decodeInt64(ba);
        }else
        if(classCode==FrameworkClassCodes.CODE_List){
            return decodeList(ba);
        }

        ISerializer serializer = getSerializer(classCode,ba.getTypeDescriptorContainer());
        if (serializer == null) {
            ba.getTypeDescriptorContainer().getTypeDescriptorByCode(classCode).getTypeClass();
        }
        if (serializer != null) {
            return serializer.decode(ba, 0);
        } else {
            System.err.println("Missing class code=" + classCode);
        }
        return null;
    }

    public static final boolean isNULL(EncodeDataContainer ba) {
        if (ba.getBytes().length > ba.getLocation() + 1
                && ba.getBytes()[ba.getLocation()] == NULL_VALUE_1
                && ba.getBytes()[ba.getLocation() + 1] == NULL_VALUE_2) {
            ba.advance(2);
            return true;
        }
        return false;
    }

    public static final void encodeNULL(byte byteArray[], int location) {
        byteArray[location] = NULL_VALUE_1;
        byteArray[location + 1] = NULL_VALUE_2;

    }

    public static final void encodeNULL(EncodeDataContainer ba) {
        ba.adjustSize(2);
        encodeNULL(ba.getBytes(), ba.getLocation());
        ba.advance(2);
    }

    public static final Object decodeAndObject(EncodeDataContainer ba) {
        if (isNULL(ba)) {
            return null;
        }
        int classCode = decodeInt16(ba);
        ISerializer serializer = getSerializer(classCode,ba.getTypeDescriptorContainer());
        if (serializer == null) {
            ba.getTypeDescriptorContainer().getTypeDescriptorByCode(classCode).getTypeClass();
        }
        if (serializer != null) {
            if(ba.getSubElementsData().get(classCode)!=null){
                EncodeDataContainer subBA = ba.getSubElementsData().get(classCode).get(0);
                subBA.advance(2);
                return serializer.decode(subBA, 0);
            }else
                return null;
        } else {
            System.err.println("Missing class code=" + classCode);
        }
        return null;
    }

    public static final void encodeIntArray(int value[], EncodeDataContainer ba) {
        if (value == null) {
            encodeNULL(ba);
        } else {
            encodeInt32(value.length, ba);
            for (int i = 0; i < value.length; i++) {
                encodeInt32(value[i], ba);
            }
        }
    }

    public static final int[] decodeIntArray(EncodeDataContainer ba) {
        if (isNULL(ba)) {
            return null;
        }
        int size = decodeInt32(ba);
        int result[] = new int[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = decodeInt32(ba);
        }
        return result;
    }

    public static final void encodeArray(Object value[], EncodeDataContainer ba,Class<?> componentType) {
        if (value == null) {
            encodeNULL(ba);
        } else {
            encodeInt32(value.length, ba);
            for (int i = 0; i < value.length; i++) {
                if (String.class.equals(componentType)) {
                    encodeString((String) value[i], ba);
                } else if (ISerializer.class.isAssignableFrom(componentType)) {
                    ((ISerializer) value[i]).encode(value[i], ba);
                } else {
                    encodeObject(value[i], ba, componentType);
                }
            }
        }
    }

    public static final void encodeList(List<?> list, EncodeDataContainer ba) {
        if (list == null) {
            encodeNULL(ba);
        } else {
            encodeInt32(list.size(), ba);
            for (Object o: list) {
                encodeObject(o, ba, null);
            }
        }
    }

    public static final void encodeList(List<?> list, EncodeDataContainer ba,Class<?> componentType) {
        if (list == null) {
            encodeNULL(ba);
        } else {
            encodeInt32(list.size(), ba);
            for (Object o: list) {
                if (String.class.equals(componentType)) {
                    encodeString((String) o, ba);
                } else if (ISerializer.class.isAssignableFrom(componentType)) {
                    ((ISerializer) o).encode(o, ba);
                } else {
                    encodeObject(o, ba, componentType);
                }
            }
        }
    }

    public static final void encodeAndAddList(List<?> list, EncodeDataContainer ba,Class<?> componentType) {
        if (list == null) {
            encodeNULL(ba);
        } else {
            encodeInt32(list.size(), ba);
            int componentCode = ba.getTypeDescriptorContainer().getTypeDescriptorByClass(componentType).getClassCode();
            for (Object o: list) {
                EncodeDataContainer subBA = new EncodeDataContainer(1024,ba.getTypeDescriptorContainer());
                if (String.class.equals(componentType)) {
                    encodeString((String) o, subBA);
                } else if (ISerializer.class.isAssignableFrom(componentType)) {
                    ((ISerializer) o).encode(o, subBA);
                } else {
                    encodeObject(o, subBA, componentType);
                }
                ba.addSubElementData(componentCode, subBA,o);
            }
        }
    }

    public static final Object[] decodeArray(EncodeDataContainer ba,Class<?> componentType) {
        if (isNULL(ba)) {
            return null;
        }
        int size = decodeInt32(ba);
        Object result[] = (Object[]) Array.newInstance(componentType, size);
        for (int i = 0; i < result.length; i++) {
            if (String.class.equals(componentType)) {
                result[i] = decodeString(ba);
            } else if (ISerializer.class.isAssignableFrom(componentType)) {
                ISerializer serializer = getSerializer(componentType,ba.getTypeDescriptorContainer());
                if (serializer != null) {
                    result[i] = serializer.decode(ba, 0);
                } else {
                    System.err.println("Can't find Serializer for class:"
                            + componentType.getName());
                }
            } else {
                result[i] = decodeObject(ba);
            }
        }
        return result;
    }

    public static final List<Object> decodeList(EncodeDataContainer ba) {
        if (isNULL(ba)) {
            return null;
        }
        int size = decodeInt32(ba);
        List<Object> result = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++) {
            result.add(decodeObject(ba));
        }
        return result;
    }

    public static final List<Object> decodeList(EncodeDataContainer ba,Class<?> componentType) {
        if (isNULL(ba)) {
            return null;
        }
        int size = decodeInt32(ba);
        List<Object> result = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++) {
            if (int.class.equals(componentType)) {
                result.add(decodeInt32(ba));
            } else if (long.class.equals(componentType)) {
                result.add(decodeInt64(ba));
            } else if (boolean.class.equals(componentType)) {
                result.add(decodeBoolean(ba));
            } else if (String.class.equals(componentType)) {
                result.add(decodeString(ba));
            } else if (ISerializer.class.isAssignableFrom(componentType)) {
                ISerializer serializer = getSerializer(componentType,ba.getTypeDescriptorContainer());
                if (serializer != null) {
                    result.add(serializer.decode(ba, 0));
                } else {
                    System.err.println("Can't find Serializer for class:"
                            + componentType.getName());
                }
            } else {
                result.add(decodeObject(ba));
            }
        }
        return result;
    }

    public static final List decodeAndList(EncodeDataContainer ba,Class<?> componentType) {
        if (isNULL(ba)) {
            return null;
        }
        int size = decodeInt32(ba);
        List result = new ArrayList(size);
        List<EncodeDataContainer> subElementData = ba.getSubElementsData().get(ba.getTypeDescriptorContainer().getTypeDescriptorByClass(componentType).getClassCode());
        if(subElementData!=null){
            for (int i = 0; i < subElementData.size(); i++) {
                EncodeDataContainer subBA = subElementData.get(i);
                if (int.class.equals(componentType)) {
                    result.add(decodeInt32(subBA));
                } else if (long.class.equals(componentType)) {
                    result.add(decodeInt64(subBA));
                } else if (boolean.class.equals(componentType)) {
                    result.add(decodeBoolean(subBA));
                } else if (String.class.equals(componentType)) {
                    result.add(decodeString(subBA));
                } else if (ISerializer.class.isAssignableFrom(componentType)) {
                    ISerializer serializer = getSerializer(componentType,ba.getTypeDescriptorContainer());
                    if (serializer != null) {
                        result.add(serializer.decode(subBA, 0));
                    } else {
                        System.err.println("Can't find Serializer for class:"
                                + componentType.getName());
                    }
                } else {
                    result.add(decodeObject(subBA));
                }
            }
        }
        if(result.size()==0)
            return null;
        return result;
    }

    public static String getLocalIPAddress() {
        try {
            for (NetworkInterface in : Collections.list(NetworkInterface
                    .getNetworkInterfaces())) {
                if (in.isLoopback())
                    continue;
                if (!in.isUp())
                    continue;
                if (in.isVirtual())
                    continue;
                if (in.getDisplayName().indexOf("vm") != -1)
                    continue;
                for (InterfaceAddress addr : in.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof Inet6Address)
                        continue;
                    if (!addr.getAddress().getHostAddress().startsWith("127.0")) {
                        return addr.getAddress().getHostAddress();
                    }
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    public static void main(String args[]) {

        int x = 1;
        byte[] d = new byte[4];
        encodeInt32(x, d, 0);
        int y = decodeInt32(d, 0);
        System.out.println("" + x + ":" + y);

        long x1 = Long.MIN_VALUE;
        byte[] d1 = new byte[8];
        encodeInt64(x1, d1, 0);
        long y1 = decodeInt64(d1, 0);
        System.out.println("" + x1 + ":" + y1);

        long i = -1676426779408027064L;
        byte[] b = new byte[8];
        encodeInt64(i, b, 0);
        long j = decodeInt64(b, 0);
        System.out.println(j);
    }

}
