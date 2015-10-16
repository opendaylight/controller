/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.odl.sql.jdbc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class JDBCRecordCollection {
    private List<JDBCRecord> records = new LinkedList<JDBCRecord>();
    private Object currentElement = null;
    private LinkedList<Object> objectPath = new LinkedList<>();
    public void addElementToPath(Object obj){
        this.currentElement = obj;
        objectPath.add(obj);
    }
    public Object getCurrentElement(){
        return this.currentElement;
    }
    public LinkedList<Object> getElementPath(){
        return this.objectPath;
    }
    public void setCurrentElement(Object obj){
        this.currentElement = obj;
    }
    public void addRecord(JDBCRecord record){
        this.records.add(record);
    }
    public Iterator<JDBCRecord> recordIterator(){
        return this.records.iterator();
    }
    public boolean isEmpty(){
        return this.records.isEmpty();
    }
}
