/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec.bytearray;

import java.util.List;
import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptor;

/**
 * @author - Sharon Aicler (saichler@cisco.com)
 *
 */
public class ByteArrayEncodeDataContainer extends EncodeDataContainer{
    private int location = 0;
    private byte[] bytes = null;
    public int enlargeTimes = 0;

    public ByteArrayEncodeDataContainer(byte[] _bytes,TypeDescriptor _typeDescriptor) {
        super(_typeDescriptor,EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY);
        this.bytes = _bytes;
    }

    public ByteArrayEncodeDataContainer(byte[] _bytes, Object _codingOption,TypeDescriptor _typeDescriptor) {
        super(_codingOption,_typeDescriptor,EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY);
        this.bytes = _bytes;
    }

    public ByteArrayEncodeDataContainer(int size,TypeDescriptor _typeDescriptor) {
        super(_typeDescriptor,EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY);
        this.bytes = new byte[size];
    }

    public ByteArrayEncodeDataContainer(int size, Object _codingOption,TypeDescriptor _typeDescriptor) {
        super(_codingOption,_typeDescriptor,EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY);
        this.bytes = new byte[size];
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public int getLocation() {
        return this.location;
    }

    public void advance(int size) {
        this.location += size;
    }

    public byte[] getData() {
        byte data[] = new byte[location];
        System.arraycopy(bytes, 0, data, 0, location);
        return data;
    }

    public void adjustSize(int goingToAdd) {
        if (location + goingToAdd < bytes.length)
            return;
        enlargeTimes++;
        int newSize = (int) (bytes.length * 1.3);
        if (newSize < location + goingToAdd) {
            newSize = location + goingToAdd;
        }
        byte temp[] = new byte[newSize];
        System.arraycopy(bytes, 0, temp, 0, location);
        bytes = temp;
    }

    public void resetLocation() {
        this.location = 0;
        for (List<EncodeDataContainer> list : this.getSubElementsData().values()) {
            for (EncodeDataContainer subBA : list) {
                ((ByteArrayEncodeDataContainer)subBA).resetLocation();
            }
        }
    }

    public byte[] getMarked(int mark) {
        byte[] result = new byte[location - mark];
        System.arraycopy(bytes, mark, result, 0, result.length);
        return result;
    }

    public static byte[] toSingleByteArray(ByteArrayEncodeDataContainer source){
        ByteArrayEncodeDataContainer enc = new ByteArrayEncodeDataContainer(1024,source.getTypeDescriptor());
        byte data[] = source.getData();
        source.getEncoder().encodeByteArray(data, enc);
        source.getEncoder().encodeInt16(source.getSubElementsData().size(), enc);
        for(Map.Entry<Integer,List<EncodeDataContainer>> subBAs:source.getSubElementsData().entrySet()){
            source.getEncoder().encodeInt16(subBAs.getKey(), enc);
            source.getEncoder().encodeInt32(subBAs.getValue().size(), enc);
            for(EncodeDataContainer subBA:subBAs.getValue()){
                source.getEncoder().encodeByteArray(toSingleByteArray((ByteArrayEncodeDataContainer)subBA),enc);
            }
        }
        return enc.getData();
    }

    public static ByteArrayEncodeDataContainer fromSingleByteArray(ByteArrayEncodeDataContainer source){
        ByteArrayEncodeDataContainer result = new ByteArrayEncodeDataContainer(1024,source.getTypeDescriptor());
        result.bytes = source.getEncoder().decodeByteArray(source);
        int subMapSize = source.getEncoder().decodeInt16(source);
        for(int i=0;i<subMapSize;i++){
            int code = source.getEncoder().decodeInt16(source);
            int size = source.getEncoder().decodeInt32(source);
            for(int j=0;j<size;j++){
                byte subData[] = source.getEncoder().decodeByteArray(source);
                ByteArrayEncodeDataContainer subBASource = new ByteArrayEncodeDataContainer(subData,source.getTypeDescriptorContainer().getTypeDescriptorByCode(code));
                ByteArrayEncodeDataContainer subBA = fromSingleByteArray(subBASource);
                result.addSubElementData(code, subBA,null);
            }
        }
        return result;
    }
}
