/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec;

import java.math.BigDecimal;
import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public abstract class AbstractEncoder {
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

    public abstract void encodeSize(int size,EncodeDataContainer edc);
    public abstract int decodeSize(EncodeDataContainer edc);

    public abstract void encodeInt16(int value, EncodeDataContainer edc);
    public abstract int decodeInt16(EncodeDataContainer edc);

    public abstract void encodeShort(Short value, EncodeDataContainer edc);
    public abstract short decodeShort(EncodeDataContainer edc);

    public abstract void encodeInt32(Integer value, EncodeDataContainer edc);
    public abstract int decodeInt32(EncodeDataContainer edc);

    public abstract int decodeUInt32(byte[] byteArray, int location);

    public abstract void encodeInt64(Long value, EncodeDataContainer edc);
    public abstract long decodeInt64(EncodeDataContainer edc);

    public abstract void encodeBigDecimal(BigDecimal value, EncodeDataContainer edc);
    public abstract BigDecimal decodeBigDecimal(EncodeDataContainer edc);

    public abstract long decodeUInt64(byte[] byteArray, int location);

    public abstract void encodeString(String value, EncodeDataContainer edc);
    public abstract String decodeString(EncodeDataContainer edc);

    public abstract void encodeByteArray(byte[] value, EncodeDataContainer edc);
    public abstract byte[] decodeByteArray(EncodeDataContainer edc);

    public abstract void encodeBoolean(boolean value, EncodeDataContainer edc);
    public abstract boolean decodeBoolean(EncodeDataContainer edc);

    public abstract void encodeByte(Byte value, EncodeDataContainer edc);
    public abstract byte decodeByte(EncodeDataContainer edc);

    public abstract void encodeAugmentations(Object value, EncodeDataContainer edc);
    public abstract void decodeAugmentations(Object builder, EncodeDataContainer edc,Class<?> augmentedClass);

    public abstract void encodeObject(Object value, EncodeDataContainer edc);
    public abstract void encodeObject(Object value, EncodeDataContainer edc,Class<?> objectType);
    public abstract Object decodeObject(EncodeDataContainer edc);

    public abstract boolean isNULL(EncodeDataContainer edc);
    public abstract void encodeNULL(EncodeDataContainer edc);

    public abstract void encodeAndAddObject(Object value, EncodeDataContainer edc,Class<?> objectType);
    public abstract Object decodeAndObject(EncodeDataContainer edc);

    public abstract void encodeIntArray(int value[], EncodeDataContainer edc);
    public abstract int[] decodeIntArray(EncodeDataContainer edc);

    public abstract void encodeArray(Object value[], EncodeDataContainer edc,Class<?> componentType);
    public abstract Object[] decodeArray(EncodeDataContainer edc,Class<?> componentType);

    public abstract void encodeList(List<?> list, EncodeDataContainer edc);
    public abstract void encodeList(List<?> list, EncodeDataContainer edc,Class<?> componentType);
    public abstract List<Object> decodeList(EncodeDataContainer edc);
    public abstract List<Object> decodeList(EncodeDataContainer edc,Class<?> componentType);

    public abstract void encodeAndAddList(List<?> list, EncodeDataContainer edc,Class<?> componentType);
    public abstract List decodeAndList(EncodeDataContainer edc,Class<?> componentType);

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
