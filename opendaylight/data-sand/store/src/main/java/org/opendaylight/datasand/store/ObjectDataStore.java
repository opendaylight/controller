/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.datasand.codec.ISerializer;
import org.opendaylight.datasand.codec.ThreadPool;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public abstract class ObjectDataStore {
    protected Map<String, Shard> location = new HashMap<String, Shard>();
    private static int MAX_X_VECTOR = 10;
    private static int MAX_Y_VECTOR = 10;
    private static int MAX_Z_VECTOR = 10;
    protected String dataLocation = null;
    protected boolean shouldSortFields = false;
    protected int X_VECTOR = 0;
    protected int Y_VECTOR = 0;
    protected int Z_VECTOR = 0;

    private int X_VECTOR_COUNT = 0;
    private int Y_VECTOR_COUNT = 0;
    private int Z_VECTOR_COUNT = 0;
    protected boolean closed = false;
    protected ThreadPool threadpool = new ThreadPool(4,"Object Store Database Threadpool", 2000);
    private TypeDescriptorsContainer typeContainer = null;
    private int encoderType = -1;

    public ObjectDataStore(String _dataLocation, boolean _shouldSortFields,int _encoderType) {
        this.dataLocation = _dataLocation;
        this.shouldSortFields = _shouldSortFields;
        this.encoderType = _encoderType;
        this.typeContainer = new TypeDescriptorsContainer(this.dataLocation);
        init();
    }

    public abstract void deleteDatabase();

    public String getDataLocation(){
        return this.dataLocation;
    }

    public TypeDescriptorsContainer getTypeDescriptorsContainer(){
        return this.typeContainer;
    }

    public void write(Object _object, int parentRecordIndex) {
        TypeDescriptor ctype = typeContainer.getTypeDescriptorByClass(typeContainer.getElementClass(_object));
        ISerializer s = ctype.getSerializer();
        String blockKey = s.getShardName(_object);
        if (blockKey == null) {
            blockKey = "Default";
        }
        Shard gl = getGLocation(blockKey);
        gl.writeObject(_object, ctype.getMD5IDForObject(_object), parentRecordIndex);
    }

    public void update(Object _object, int parentRecordIndex,int recordIndex) {
        TypeDescriptor ctype = typeContainer.getTypeDescriptorByClass(typeContainer.getElementClass(_object));
        ISerializer s = ctype.getSerializer();
        String blockKey = s.getShardName(_object);
        if (blockKey == null) {
            blockKey = "Default";
        }
        Shard gl = getGLocation(blockKey);
        gl.updateObject(_object,recordIndex, ctype.getMD5IDForObject(_object), parentRecordIndex);
    }

    public Object readNoChildren(Class<?> clazz, int index) {
        Shard gl = getGLocation("Default");
        TypeDescriptor table = typeContainer.getTypeDescriptorByClass(clazz);
        return gl.readObjectNoChildren(table, index);
    }

    public Object deleteNoChildren(Class<?> clazz, int index) {
        Shard gl = getGLocation("Default");
        TypeDescriptor table = typeContainer.getTypeDescriptorByClass(clazz);
        return gl.deleteObjectNoChildren(table, index);
    }

    public ObjectWithInfo readWithLocation(TypeDescriptor table, int index) {
        Shard gl = getGLocation("Default");
        Object o = gl.readObject(table.getTypeClass(), index);
        return new ObjectWithInfo(gl,table, o,index);
    }

    public ObjectWithInfo readNoChildrenWithLocation(TypeDescriptor table, int index) {
        Shard gl = getGLocation("Default");
        Object o = gl.readObjectNoChildren(table, index);
        return new ObjectWithInfo(gl,table, o,index);
    }

    public ObjectWithInfo deleteNoChildrenWithLocation(TypeDescriptor table, int index) {
        Shard gl = getGLocation("Default");
        Object o = gl.deleteObjectNoChildren(table, index);
        return new ObjectWithInfo(gl,table, o,index);
    }

    public int getEncoderType(){
        return this.encoderType;
    }

    public static class ObjectWithInfo {
        private Shard bLocation = null;
        private Object object = null;
        private int recordIndex = -1;
        private TypeDescriptor table = null;

        public ObjectWithInfo(Shard loc,TypeDescriptor tbl, Object o, int recIndex){
            this.bLocation = loc;
            this.object = o;
            this.table = tbl;
            this.recordIndex = recIndex;
        }

        public Shard getbLocation() {
            return bLocation;
        }

        public Object getObject() {
            return object;
        }

        public int getRecordIndex() {
            return recordIndex;
        }

        public TypeDescriptor getTable() {
            return table;
        }

        public ObjectWithInfo getParenInfo(){
            DataPersister df = this.bLocation.getDataPersister(this.table.getTypeClass());
            int index = df.getParentIndex(this.recordIndex);
            if(index!=-1){
                TypeDescriptor parentTable = this.table.getParent();
                Object parentObject = this.bLocation.readObjectNoChildren(parentTable, index);
                return new ObjectWithInfo(this.bLocation,parentTable,parentObject,index);
            }
            return null;
        }
    }

    public Object getEncodedKeyObject(Object dataKey){
        return dataKey;
    }

    public Object delete(Object dataKey,Class<?> clazz){
        Shard gl = getGLocation("Default");
        Object key = getEncodedKeyObject(dataKey);
        return gl.deleteObject(clazz, key);
    }

    public Object read(Object dataKey,Class<?> clazz){
        Shard gl = getGLocation("Default");
        Object key = getEncodedKeyObject(dataKey);
        return gl.readObject(clazz, key);
    }

    public Object read(Class<?> clazz, int index) {
        Shard gl = getGLocation("Default");
        return gl.readObject(clazz, index);
    }

    public Object delete(Class<?> clazz, int index) {
        Shard gl = getGLocation("Default");
        return gl.deleteObject(clazz, index);
    }

    public Object readAllGraphBySingleNode(Class<?> clazz,int index){
        Shard gl = getGLocation("Default");
        return gl.readAllObjectFromNode(clazz, index);
    }

    public abstract void init();

    public Shard getGLocation(String blockKey) {
        synchronized (location) {
            Shard loc = location.get(blockKey);
            if (loc == null) {
                Z_VECTOR_COUNT++;
                if (Z_VECTOR_COUNT > MAX_Z_VECTOR) {
                    Z_VECTOR++;
                    Z_VECTOR_COUNT = 0;
                    Y_VECTOR_COUNT++;
                }

                if (Y_VECTOR_COUNT > MAX_Y_VECTOR) {
                    Z_VECTOR = 0;
                    Y_VECTOR++;
                    Y_VECTOR_COUNT = 0;
                    X_VECTOR_COUNT++;
                }

                if (X_VECTOR_COUNT > MAX_X_VECTOR) {
                    Z_VECTOR = 0;
                    Y_VECTOR = 0;
                    X_VECTOR++;
                }
                loc = new Shard(X_VECTOR, Y_VECTOR, Z_VECTOR,blockKey,this);
                location.put(blockKey, loc);
            }
            return loc;
        }
    }

    public abstract ResultSet executeSql(String sql);
    public abstract void executeSql(String sql, PrintStream out, boolean toCsv);
    public abstract void commit();
    public abstract void close();
    public abstract boolean isClosed();
}
