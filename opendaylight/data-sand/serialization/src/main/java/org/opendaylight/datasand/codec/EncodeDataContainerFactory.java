package org.opendaylight.datasand.codec;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;

public class EncodeDataContainerFactory {

    private static Map<Integer,EncodeDataContainerInstantiator> instantiators = new HashMap<Integer, EncodeDataContainerInstantiator>();

    public static interface EncodeDataContainerInstantiator {
        public EncodeDataContainer newEncodeDataContainer(Object data,Object key,TypeDescriptorsContainer _tsc);
    }

    public static void registerInstantiator(int type,EncodeDataContainerInstantiator i){
        instantiators.put(type, i);
    }

    public static EncodeDataContainer newContainer(Object data,Object key,int _type,TypeDescriptorsContainer _tsc){
        switch(_type){
            case EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY:
                if(data==null)
                    data = new byte[1024];
                ByteArrayEncodeDataContainer bac = new ByteArrayEncodeDataContainer((byte[])data,_tsc);
                bac.setMD5ID((MD5Identifier)key);
                return bac;
        }
        EncodeDataContainerInstantiator i = instantiators.get(_type);
        if(i!=null)
            return i.newEncodeDataContainer(data,key,_tsc);
        return null;
    }
}
