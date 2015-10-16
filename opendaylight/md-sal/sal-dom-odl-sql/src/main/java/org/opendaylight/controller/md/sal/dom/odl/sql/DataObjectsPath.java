/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.odl.sql;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yangtools.yang.binding.DataObject;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class DataObjectsPath {
    private List<DataObject> dataObjectPath = new ArrayList<DataObject>();

    public void addDataObject(DataObject value){
        dataObjectPath.add(value);
    }
    /*
    public List<DataObject> getDataObjectPath(){
        return this.dataObjectPath;
    }*/

    public DataObject getSelectedObject(){
        if(dataObjectPath.size()>0){
            return dataObjectPath.get(0);
        }
        return null;
    }

    public DataObject getSelectedObjectParent(){
        if(dataObjectPath.size()>1){
            return dataObjectPath.get(1);
        }
        return null;
    }
}
