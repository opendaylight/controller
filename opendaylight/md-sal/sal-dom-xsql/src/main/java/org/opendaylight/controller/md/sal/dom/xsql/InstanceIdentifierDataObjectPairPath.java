/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.xsql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class InstanceIdentifierDataObjectPairPath<K,V> {
    private List<Map.Entry<K, V>> pairPath = new ArrayList<>();

    public void addPair(K key,V value){
        KeyValuePair<K, V> pair = new KeyValuePair<K,V>(key, value);
        pairPath.add(pair);
    }

    public List<Map.Entry<K, V>> getPairPath(){
        return this.pairPath;
    }

    private static class KeyValuePair<K,V> implements Map.Entry<K, V> {
        private K key = null;
        private V value = null;
        public KeyValuePair(K _key,V _value){
            this.key = _key;
            this.value = _value;
        }
        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            return null;
        }
    }
}
