/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.sqlite;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptor;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class SQLiteEncodeDataContainer extends EncodeDataContainer{

    static{
        EncodeDataContainer.registerEncoder(EncodeDataContainer.ENCODER_TYPE_SQLITE, new SQLiteEncoder());
    }

    private List<Object> list = new ArrayList<Object>();
    private int location = 0;

    public SQLiteEncodeDataContainer(TypeDescriptor _typeDescrptor){
        super(_typeDescrptor,EncodeDataContainer.ENCODER_TYPE_SQLITE);
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
