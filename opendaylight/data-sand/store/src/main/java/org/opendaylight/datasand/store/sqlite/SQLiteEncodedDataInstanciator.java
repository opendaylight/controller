package org.opendaylight.datasand.store.sqlite;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.EncodeDataContainerFactory.EncodeDataContainerInstantiator;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;

public class SQLiteEncodedDataInstanciator implements EncodeDataContainerInstantiator{
    @Override
    public EncodeDataContainer newEncodeDataContainer(Object data, Object key,TypeDescriptorsContainer _tsc) {
        return new SQLiteEncodeDataContainer(_tsc);
    }
}
