package org.opendaylight.datasand.codec;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;

public class EncodeDataContainerFactory {
    public static EncodeDataContainer newContainer(Object data,Object key,int _type,TypeDescriptorsContainer _tsc){
        switch(_type){
            case EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY:
                if(data==null)
                    data = new byte[1024];
                ByteArrayEncodeDataContainer bac = new ByteArrayEncodeDataContainer((byte[])data,_tsc);
                bac.setMD5ID((MD5Identifier)key);
                return bac;
        }
        return null;
    }
}
