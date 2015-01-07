/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.sqlite;

import java.math.BigDecimal;
import java.util.List;

import org.opendaylight.datasand.codec.AbstractEncoder;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class SQLiteEncoder extends AbstractEncoder{

    @Override
    public void encodeSize(int size, EncodeDataContainer edc) {
    }

    @Override
    public int decodeSize(EncodeDataContainer edc) {
        return 0;
    }

    @Override
    public void encodeInt16(int value, EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        ba.addObject(value);
    }

    @Override
    public int decodeInt16(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getObject();
        if(o!=null)
            return (Integer)o;
        return 0;
    }

    @Override
    public void encodeShort(Short value, EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        ba.addObject(value);
    }

    @Override
    public short decodeShort(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getObject();
        if(o!=null)
            return (Short)o;
        return 0;
    }

    @Override
    public void encodeInt32(Integer value, EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        ba.addObject(value);
    }

    @Override
    public int decodeInt32(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getObject();
        if(o!=null)
            return (Integer)o;
        return 0;
    }

    @Override
    public int decodeUInt32(byte[] byteArray, int location) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeInt64(Long value, EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        ba.addObject(value);
    }

    @Override
    public long decodeInt64(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getObject();
        if(o!=null)
            return (Long)o;
        return 0;
    }

    @Override
    public void encodeBigDecimal(BigDecimal value, EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public BigDecimal decodeBigDecimal(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long decodeUInt64(byte[] byteArray, int location) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeString(String value, EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        ba.addObject(value);
    }

    @Override
    public String decodeString(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getObject();
        if(o!=null)
            return (String)o;
        return null;
    }

    @Override
    public void encodeByteArray(byte[] value, EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] decodeByteArray(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeBoolean(boolean value, EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        if(value)
            ba.addObject("T");
        else
            ba.addObject("F");
    }

    @Override
    public boolean decodeBoolean(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getObject();
        if(o!=null){
            return (Boolean)o;
        }
        return false;
    }

    @Override
    public void encodeByte(Byte value, EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte decodeByte(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeAugmentations(Object value, EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public void decodeAugmentations(Object builder, EncodeDataContainer ba,
            Class<?> augmentedClass) {
        // TODO Auto-generated method stub

    }

    @Override
    public void encodeObject(Object value, EncodeDataContainer ba,Class<?> objectType) {
        ISerializer serializer = ba.getTypeDescriptorContainer().getTypeDescriptorByObject(value).getSerializer();
        serializer.encode(value, ba);
    }

    @Override
    public Object decodeObject(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isNULL(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void encodeNULL(EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public void encodeAndAddObject(Object value, EncodeDataContainer ba,
            Class<?> objectType) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object decodeAndObject(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeIntArray(int[] value, EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public int[] decodeIntArray(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeArray(Object[] value, EncodeDataContainer ba,
            Class<?> componentType) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object[] decodeArray(EncodeDataContainer ba, Class<?> componentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeList(List<?> list, EncodeDataContainer ba) {
        // TODO Auto-generated method stub

    }

    @Override
    public void encodeList(List<?> list, EncodeDataContainer ba,
            Class<?> componentType) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Object> decodeList(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Object> decodeList(EncodeDataContainer ba,
            Class<?> componentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeAndAddList(List<?> list, EncodeDataContainer ba,
            Class<?> componentType) {
        // TODO Auto-generated method stub

    }

    @Override
    public List decodeAndList(EncodeDataContainer ba, Class<?> componentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeObject(Object value, EncodeDataContainer edc) {
        // TODO Auto-generated method stub
    }
}
