package org.opendaylight.datasand.store.sqlite;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;

public class SQLiteEncodeDataContainer extends EncodeDataContainer{

    static{
        EncodeDataContainer.registerEncoder(EncodeDataContainer.ENCODER_TYPE_SQLITE, new SQLiteEncoder());
    }

    private List<Object> list = new ArrayList<Object>();
    private int location = 0;

    public SQLiteEncodeDataContainer(TypeDescriptorsContainer _container){
        super(_container,EncodeDataContainer.ENCODER_TYPE_SQLITE);
    }

    @Override
    public void resetLocation() {
        location = 0;
    }

    public void addObject(Object o){
        list.add(location, o);
        location++;
    }

    public Object getObject(){
        if(location<list.size()){
            Object o = list.get(location);
            location++;
            return o;
        }else
            return null;
    }
}
