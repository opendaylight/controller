package org.opendaylight.datasand.codec;

import java.math.BigDecimal;
import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEncoder {
    public static final byte NULL_VALUE_1 = (byte) 'N';
    public static final byte NULL_VALUE_2 = (byte) 'L';
    public static Map<Class<?>,ISerializer> registeredSerializers = new ConcurrentHashMap<Class<?>,ISerializer>();
    public static Map<Integer,Class<?>>  registeredSerializersID = new ConcurrentHashMap<Integer,Class<?>>();
    public static Map<Class<?>,Integer> registeredSerializersClassCode = new ConcurrentHashMap<Class<?>,Integer>();

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

    public abstract void encodeInt16(int value, EncodeDataContainer ba);
    public abstract int decodeInt16(EncodeDataContainer ba);

    public abstract void encodeShort(Short value, EncodeDataContainer ba);
    public abstract short decodeShort(EncodeDataContainer ba);

    public abstract void encodeInt32(Integer value, EncodeDataContainer ba);
    public abstract int decodeInt32(EncodeDataContainer ba);

    public abstract int decodeUInt32(byte[] byteArray, int location);

    public abstract void encodeInt64(Long value, EncodeDataContainer ba);
    public abstract long decodeInt64(EncodeDataContainer ba);

    public abstract void encodeBigDecimal(BigDecimal value, EncodeDataContainer ba);
    public abstract BigDecimal decodeBigDecimal(EncodeDataContainer ba);

    public abstract long decodeUInt64(byte[] byteArray, int location);

    public abstract void encodeString(String value, EncodeDataContainer ba);
    public abstract String decodeString(EncodeDataContainer ba);

    public abstract void encodeByteArray(byte[] value, EncodeDataContainer ba);
    public abstract byte[] decodeByteArray(EncodeDataContainer ba);

    public abstract void encodeBoolean(boolean value, EncodeDataContainer ba);
    public abstract boolean decodeBoolean(EncodeDataContainer ba);

    public abstract void encodeByte(Byte value, EncodeDataContainer ba);
    public abstract byte decodeByte(EncodeDataContainer ba);

    public abstract void encodeAugmentations(Object value, EncodeDataContainer ba);
    public abstract void decodeAugmentations(Object builder, EncodeDataContainer ba,Class<?> augmentedClass);

    public abstract void encodeObject(Object value, EncodeDataContainer ba,Class<?> objectType);
    public abstract Object decodeObject(EncodeDataContainer ba);

    public abstract boolean isNULL(EncodeDataContainer ba);
    public abstract void encodeNULL(EncodeDataContainer ba);

    public abstract void encodeAndAddObject(Object value, EncodeDataContainer ba,Class<?> objectType);
    public abstract Object decodeAndObject(EncodeDataContainer ba);

    public abstract void encodeIntArray(int value[], EncodeDataContainer ba);
    public abstract int[] decodeIntArray(EncodeDataContainer ba);

    public abstract void encodeArray(Object value[], EncodeDataContainer ba,Class<?> componentType);
    public abstract Object[] decodeArray(EncodeDataContainer ba,Class<?> componentType);

    public abstract void encodeList(List<?> list, EncodeDataContainer ba);
    public abstract void encodeList(List<?> list, EncodeDataContainer ba,Class<?> componentType);
    public abstract List<Object> decodeList(EncodeDataContainer ba);
    public abstract List<Object> decodeList(EncodeDataContainer ba,Class<?> componentType);

    public abstract void encodeAndAddList(List<?> list, EncodeDataContainer ba,Class<?> componentType);
    public abstract List decodeAndList(EncodeDataContainer ba,Class<?> componentType);

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
}
