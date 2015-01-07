/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.bytearray;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class ByteArrayDataLocation {

    private int parentIndex = -1;
    private int recordIndex = -1;
    private int startPosition = -1;
    private int length = -1;

    public ByteArrayDataLocation() {
    }

    public ByteArrayDataLocation(int _startPosition, int _length,int _recordIndex,int _parentIndex) {
        this.startPosition = _startPosition;
        this.length = _length;
        this.recordIndex = _recordIndex;
        this.parentIndex = _parentIndex;
    }

    public int getStartPosition() {
        return this.startPosition;
    }

    public int getLength() {
        return this.length;
    }

    public int getRecordIndex(){
        return this.recordIndex;
    }

    public int getParentIndex(){
        return this.parentIndex;
    }

    public void updateLocationInfo(int _startPosition,int _length){
        this.startPosition = _startPosition;
        this.length = _length;
    }

    public void encode(byte[] byteArray, int location) {
        ByteEncoder.encodeInt32(this.startPosition, byteArray, location);
        ByteEncoder.encodeInt32(this.length, byteArray, location + 4);
        ByteEncoder.encodeInt32(this.recordIndex, byteArray,location+8);
        ByteEncoder.encodeInt32(this.parentIndex, byteArray,location+12);
    }

    public void encode(EncodeDataContainer _ba) {
        ByteArrayEncodeDataContainer ba = (ByteArrayEncodeDataContainer)_ba;
        ba.adjustSize(16);
        encode(ba.getBytes(), ba.getLocation());
        ba.advance(16);
    }

    public static ByteArrayDataLocation decode(byte[] byteArray, int location, int length) {
        return new ByteArrayDataLocation(ByteEncoder.decodeInt32(byteArray,location),
                                ByteEncoder.decodeInt32(byteArray, location + 4),
                                ByteEncoder.decodeInt32(byteArray, location + 8),
                                ByteEncoder.decodeInt32(byteArray, location + 12));
    }

    public static ByteArrayDataLocation decode(EncodeDataContainer _ba, int length) {
        ByteArrayEncodeDataContainer ba = (ByteArrayEncodeDataContainer)_ba;
        ByteArrayDataLocation result = decode(ba.getBytes(), ba.getLocation(), length);
        ba.advance(16);
        return result;
    }
}
