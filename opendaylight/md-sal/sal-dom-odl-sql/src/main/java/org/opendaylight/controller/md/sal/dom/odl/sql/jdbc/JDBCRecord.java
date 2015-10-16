/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.odl.sql.jdbc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class JDBCRecord {
    private Map<String, Object> data = new HashMap<>();
    private LinkedList<Object> objectPath = new LinkedList<>();
    private boolean fitCriteria = true;
    private Object currentElement = null;

    public Map<String, Object> getRecord() {
        return this.data;
    }
    public void setElementPath(LinkedList<Object> path){
        this.objectPath = path;
        this.currentElement = path.getLast();
    }
    public LinkedList<Object> getElementPath(){
        return this.objectPath;
    }
    public void setFitCriteria(boolean b){
        this.fitCriteria = b;
    }
    public boolean isFitCriteria(){
        return this.fitCriteria;
    }
    public void setValue(String key,Object value){
        this.data.put(key, value);
    }
    public Object getCurrentElement(){
        return this.currentElement;
    }
    public Map<String,Object> getDataMap(){
        return this.data;
    }
    public void setDataMap(JDBCRecord rec){
        this.data = rec.data;
    }
    public void addAllData(JDBCRecord rec){
        this.data.putAll(rec.data);
    }
}
