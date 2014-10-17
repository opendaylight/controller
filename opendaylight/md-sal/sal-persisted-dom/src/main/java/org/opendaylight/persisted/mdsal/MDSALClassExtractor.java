package org.opendaylight.persisted.mdsal;

import org.opendaylight.persisted.codec.IClassExtractor;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSALClassExtractor implements IClassExtractor{

    @Override
    public Class<?> getObjectClass(Object obj) {
        if(obj instanceof DataObject){
            return ((DataObject)obj).getImplementedInterface();
        }
        return obj.getClass();
    }
}
