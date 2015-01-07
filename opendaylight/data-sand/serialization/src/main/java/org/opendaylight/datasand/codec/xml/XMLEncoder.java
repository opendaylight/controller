/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec.xml;

import java.math.BigDecimal;
import java.util.List;

import org.opendaylight.datasand.codec.AbstractEncoder;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptor;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 *
 */
public class XMLEncoder extends AbstractEncoder {

    private void addEntry(Object value,EncodeDataContainer edc){
        XMLEncodeDataContainer xml = (XMLEncodeDataContainer)edc;
        xml.addEntry(xml.getCurrentAttributeName(),value);
    }

    @Override
    public void encodeSize(int size, EncodeDataContainer edc) {
    }

    @Override
    public int decodeSize(EncodeDataContainer edc) {
        return 0;
    }

    @Override
    public void encodeInt16(int value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public int decodeInt16(EncodeDataContainer edc) {
        return 0;
    }

    @Override
    public void encodeShort(Short value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public short decodeShort(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeInt32(Integer value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public int decodeInt32(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int decodeUInt32(byte[] byteArray, int location) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeInt64(Long value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public long decodeInt64(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeBigDecimal(BigDecimal value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public BigDecimal decodeBigDecimal(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long decodeUInt64(byte[] byteArray, int location) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeString(String value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public String decodeString(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeByteArray(byte[] value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public byte[] decodeByteArray(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeBoolean(boolean value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public boolean decodeBoolean(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void encodeByte(Byte value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public byte decodeByte(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void encodeAugmentations(Object value, EncodeDataContainer edc) {
        // TODO Auto-generated method stub

    }

    @Override
    public void decodeAugmentations(Object builder, EncodeDataContainer edc,
            Class<?> augmentedClass) {
        // TODO Auto-generated method stub

    }

    @Override
    public void encodeObject(Object value, EncodeDataContainer edc) {
        TypeDescriptor td = edc.getTypeDescriptorContainer().getTypeDescriptorByObject(value);
        XMLEncodeDataContainer xml = (XMLEncodeDataContainer)edc;
        if(td!=edc.getTypeDescriptor()){
            xml = new XMLEncodeDataContainer(td);
            edc.addSubElementData(td.getClassCode(), xml, value);
        }
        td.getSerializer().encode(value, xml);
    }

    @Override
    public void encodeObject(Object value, EncodeDataContainer edc,Class<?> objectType) {
        this.encodeObject(value, edc);
    }

    @Override
    public Object decodeObject(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isNULL(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void encodeNULL(EncodeDataContainer edc) {
        addEntry("", edc);
    }

    @Override
    public void encodeAndAddObject(Object value, EncodeDataContainer edc,Class<?> objectType) {
        encodeObject(value, edc);
    }

    @Override
    public Object decodeAndObject(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeIntArray(int[] value, EncodeDataContainer edc) {
        addEntry(value, edc);
    }

    @Override
    public int[] decodeIntArray(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeArray(Object[] value, EncodeDataContainer edc,
            Class<?> componentType) {
        addEntry(value, edc);
    }

    @Override
    public Object[] decodeArray(EncodeDataContainer edc, Class<?> componentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeList(List<?> list, EncodeDataContainer edc) {
        addEntry(list, edc);
    }

    @Override
    public void encodeList(List<?> list, EncodeDataContainer edc,
            Class<?> componentType) {
        addEntry(list, edc);
    }

    @Override
    public List<Object> decodeList(EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Object> decodeList(EncodeDataContainer edc,
            Class<?> componentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeAndAddList(List<?> list, EncodeDataContainer edc,Class<?> componentType) {
        if (list == null) {
            return;
        } else {
            int componentCode = edc.getTypeDescriptorContainer().getTypeDescriptorByClass(componentType).getClassCode();
            for (Object o: list) {
                XMLEncodeDataContainer subBA = new XMLEncodeDataContainer(edc.getTypeDescriptorContainer().getTypeDescriptorByCode(componentCode));
                encodeObject(o, subBA, componentType);
                edc.addSubElementData(componentCode, subBA,o);
            }
        }
    }

    @Override
    public List decodeAndList(EncodeDataContainer edc, Class<?> componentType) {
        // TODO Auto-generated method stub
        return null;
    }
}
