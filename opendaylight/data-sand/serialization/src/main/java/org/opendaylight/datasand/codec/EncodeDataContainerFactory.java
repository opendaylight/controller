/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class EncodeDataContainerFactory {

    private static Map<Integer,EncodeDataContainerInstantiator> instantiators = new HashMap<Integer, EncodeDataContainerInstantiator>();

    public static interface EncodeDataContainerInstantiator {
        public EncodeDataContainer newEncodeDataContainer(Object data,Object key,TypeDescriptor _ts);
    }

    public static void registerInstantiator(int type,EncodeDataContainerInstantiator i){
        instantiators.put(type, i);
    }

    public static EncodeDataContainer newContainer(Object data,Object key,int _type,TypeDescriptor _ts){
        switch(_type){
            case EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY:
                if(data==null)
                    data = new byte[1024];
                ByteArrayEncodeDataContainer bac = new ByteArrayEncodeDataContainer((byte[])data,_ts);
                bac.setMD5ID((MD5Identifier)key);
                return bac;
        }
        EncodeDataContainerInstantiator i = instantiators.get(_type);
        if(i!=null)
            return i.newEncodeDataContainer(data,key,_ts);
        return null;
    }
}
