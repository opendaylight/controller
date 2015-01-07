/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.store.DataPersister;
import org.opendaylight.datasand.store.Shard;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class SQLiteDataPersister extends DataPersister{

    private Connection conn = null;
    private int lastRecordIndex = -1;

    public static Connection getConnection(){
        try {
          Class.forName("org.sqlite.JDBC");
          return DriverManager.getConnection("jdbc:sqlite:"+SQLiteObjectStore.DBName);
        }catch (Exception err){
            err.printStackTrace();
        }
        return null;
    }

    public SQLiteDataPersister(Shard _shard,Class<?> _cls,TypeDescriptorsContainer _container){
        super(_shard,_cls,_container);
        conn = getConnection();
        checkAndCreateTable();
    }

    private void checkAndCreateTable(){
        String tableName = this.type.getSimpleName();
        Statement st = null;
        ResultSet rs = null;
        boolean tableExist = false;
        try{
            st = conn.createStatement();
            rs = st.executeQuery("select count(*) from "+tableName);
            tableExist = true;
        }catch(Exception err){
        }finally{
            if(rs!=null)try{rs.close();}catch(Exception err){}rs=null;
            if(st!=null)try{st.close();}catch(Exception err){}st=null;
        }
        if(!tableExist){
            String sql="create table "+tableName+" (rec_index INT,parent_index INT";
            TypeDescriptor ts = this.container.getTypeDescriptorByClass(this.type);
            for(AttributeDescriptor ad:ts.getAttributes()){
                sql+=",";
                String name = ad.getColumnName();
                sql+=name;
                if(ad.getReturnType().equals(String.class)){
                    sql+=" TEXT";
                }else
                if(ad.getReturnType().equals(int.class) || ad.getReturnType().equals(Integer.class)){
                    sql+=" INT";
                }else
                if(ad.getReturnType().equals(long.class) || ad.getReturnType().equals(Long.class)){
                    sql+=" REAL";
                }else
                if(ad.getReturnType().equals(short.class) || ad.getReturnType().equals(Short.class)){
                    sql+=" REAL";
                }else
                if(ad.getReturnType().equals(boolean.class) || ad.getReturnType().equals(Boolean.class)){
                    sql+=" char(1)";
                }else{
                    System.err.print("Error!");
                    sql+=" TEXT";
                }
            }
            sql+=")";
            try{
                st = conn.createStatement();
                rs = st.executeQuery(sql);
            }catch(Exception err){
                err.printStackTrace();
            }finally{
                if(rs!=null)try{rs.close();}catch(Exception err){}rs=null;
                if(st!=null)try{st.close();}catch(Exception err){}st=null;
            }
        }
    }

    @Override
    public void save() {
        // TODO Auto-generated method stub

    }

    @Override
    public void load() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getObjectCount() {
        Statement st = null;
        ResultSet rs = null;
        try{
            st = conn.createStatement();
            rs = st.executeQuery("select count(*) from "+this.type.getSimpleName());
            rs.next();
            return rs.getInt(1);
        }catch(Exception err){
            err.printStackTrace();
        }finally{
            if(rs!=null)try{rs.close();}catch(Exception err){}rs=null;
            if(st!=null)try{st.close();}catch(Exception err){}st=null;
        }
        return 0;
    }

    @Override
    public boolean contain(Object key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDeleted(Object data) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int write(int parentRecordIndex, int recordIndex,EncodeDataContainer _ba) {
        SQLiteEncodeDataContainer ba = (SQLiteEncodeDataContainer)_ba;
        if(recordIndex==-1){
            lastRecordIndex++;
            recordIndex = lastRecordIndex;
            StringBuffer sql = new StringBuffer("Insert Into ");
            StringBuffer values = new StringBuffer(" values(");
            sql.append(this.type.getSimpleName());
            sql.append(" (rec_index,parent_index");
            values.append(recordIndex).append(",").append(parentRecordIndex);
            TypeDescriptor ts = this.container.getTypeDescriptorByClass(this.type);
            for(AttributeDescriptor ad:ts.getAttributes()){
                sql.append(",");
                values.append(",");
                sql.append(ad.getColumnName());
                ba.setCurrentAttributeName(ad.getColumnName());
                Object value = ba.getEntryValue(ba);
                String sValue = "";
                if(value!=null)
                    sValue = value.toString();
                if(ad.getReturnType().equals(String.class)){
                    values.append("'").append(sValue).append("'");
                }else
                if(ad.getReturnType().equals(int.class) || ad.getReturnType().equals(Integer.class)){
                    values.append(sValue);
                }else
                if(ad.getReturnType().equals(long.class) || ad.getReturnType().equals(Long.class)){
                    values.append(sValue);
                }else
                if(ad.getReturnType().equals(short.class) || ad.getReturnType().equals(Short.class)){
                    values.append(sValue);
                }else
                if(ad.getReturnType().equals(boolean.class) || ad.getReturnType().equals(Boolean.class)){
                    values.append("'").append(sValue).append("'");
                }else{
                    values.append("'").append(sValue).append("'");
                }
            }
            sql.append(")").append(values).append(")");
            try{
                conn.createStatement().execute(sql.toString());
            }catch(Exception err){
                err.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public Object delete(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object delete(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] delete(int[] recordIndexs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object read(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object read(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] read(int[] recordIndexs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getParentIndex(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getIndexByKey(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getParentIndexByKey(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int[] getRecordIndexesByParentIndex(int parentRecordIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }
}
