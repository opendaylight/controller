package org.opendaylight.controller.md.sal.dom.xsql.jdbc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
