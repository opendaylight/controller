package org.opendaylight.persisted.mdsal;

import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.observers.IClassExtractorObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSALClassExtractor implements IClassExtractorObserver{

    @Override
    public Class<?> getObjectClass(Object obj) {
        if(obj instanceof DataObject){
            return ((DataObject)obj).getImplementedInterface();
        }
        return obj.getClass();
    }

    @Override
    public Class<?> getBuilderClass(TypeDescriptor td) {
        if(DataObject.class.isAssignableFrom(td.getTypeClass())){
            try{
                return getClass().getClassLoader().loadClass(td.getTypeClassName()+"Builder");
            }catch(Exception err){}
        }
        return null;
    }

    @Override
    public String getBuilderMethod(TypeDescriptor td) {
        if(DataObject.class.isAssignableFrom(td.getTypeClass())){
            return "build()";
        }
        return null;
    }
}
