/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cmap;

import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class CMapEntry<K,V> implements Map.Entry,ISerializer{
    private K key = null;
    private V value = null;
    public CMapEntry(){}
    public CMapEntry(K _key,V _value){
        this.key = _key;
        this.value = _value;
    }
    public K getKey() {
        return key;
    }
    public V getValue() {
        return value;
    }
    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }
    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        CMapEntry<K,V> me = (CMapEntry<K,V>)value;
        ba.getEncoder().encodeObject(me.getKey(), ba);
        ba.getEncoder().encodeObject(me.getValue(), ba);
    }
    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }
    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        Object key = ba.getEncoder().decodeObject(ba);
        Object value = ba.getEncoder().decodeObject(ba);
        return new CMapEntry<K,V>((K)key,(V)value);
    }
    @Override
    public String getShardName(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Object getRecordKey(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Object setValue(Object value) {
        // TODO Auto-generated method stub
        return null;
    }
}
