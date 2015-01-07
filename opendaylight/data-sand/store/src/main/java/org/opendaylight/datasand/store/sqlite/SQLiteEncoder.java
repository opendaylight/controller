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
import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class SQLiteEncoder extends AbstractEncoder{

    private void addEntry(Object value,EncodeDataContainer edc){
        SQLiteEncodeDataContainer sql = (SQLiteEncodeDataContainer)edc;
        sql.addEntry(sql.getCurrentAttributeName(),value);
    }

    @Override
    public void encodeSize(int size, EncodeDataContainer edc) {
    }

    @Override
    public int decodeSize(EncodeDataContainer edc) {
        return 0;
    }

    @Override
    public void encodeInt16(int value, EncodeDataContainer _ba) {
        this.addEntry(value, _ba);
    }

    @Override
    public int decodeInt16(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getEntryValue(ba);
        if(o!=null)
            return (Integer)o;
        return 0;
    }

    @Override
    public void encodeShort(Short value, EncodeDataContainer _ba) {
        this.addEntry(value, _ba);
    }

    @Override
    public short decodeShort(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getEntryValue(ba);
        if(o!=null)
            return (Short)o;
        return 0;
    }

    @Override
    public void encodeInt32(Integer value, EncodeDataContainer _ba) {
        this.addEntry(value, _ba);
    }

    @Override
    public int decodeInt32(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getEntryValue(ba);
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
        this.addEntry(value, _ba);
    }

    @Override
    public long decodeInt64(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getEntryValue(ba);
        if(o!=null)
            return (Long)o;
        return 0;
    }

    @Override
    public void encodeBigDecimal(BigDecimal value, EncodeDataContainer ba) {
        this.addEntry(value.toString(), ba);
    }

    @Override
    public BigDecimal decodeBigDecimal(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getEntryValue(ba);
        if(o!=null)
            return new BigDecimal(o.toString());
        return null;
    }

    @Override
    public long decodeUInt64(byte[] byteArray, int location) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeString(String value, EncodeDataContainer _ba) {
        this.addEntry(value, _ba);
    }

    @Override
    public String decodeString(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        Object o = ba.getEntryValue(ba);
        if(o!=null)
            return (String)o;
        return null;
    }

    @Override
    public void encodeByteArray(byte[] value, EncodeDataContainer ba) {
    }

    @Override
    public byte[] decodeByteArray(EncodeDataContainer ba) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeBoolean(boolean value, EncodeDataContainer _ba) {
        if(value)
            this.addEntry("T", _ba);
        else
            this.addEntry("F", _ba);
    }

    @Override
    public boolean decodeBoolean(EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        String v = (String)ba.getEntryValue(ba);
        if("T".equals(v))
            return true;
        return false;
    }

    @Override
    public void encodeByte(Byte value, EncodeDataContainer ba) {
    }

    @Override
    public byte decodeByte(EncodeDataContainer ba) {
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
        //Need to validate this
        AttributeDescriptor at = ba.getTypeDescriptor().getAttributeByAttributeName(ba.getCurrentAttributeName());
        ISerializer serializer = ba.getTypeDescriptorContainer().getTypeDescriptorByClass(at.getReturnType()).getSerializer();
        return serializer.decode(ba,0);
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
