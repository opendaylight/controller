/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.bytearray;

import java.io.File;
import java.io.PrintStream;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.EncodeDataContainerFactory;
import org.opendaylight.datasand.codec.MD5Identifier;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.store.ObjectDataStore;
import org.opendaylight.datasand.store.Shard;
import org.opendaylight.datasand.store.jdbc.DataSandJDBCResultSet;
import org.opendaylight.datasand.store.jdbc.DataSandJDBCServer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class ByteArrayObjectDataStore extends ObjectDataStore{

    private DataSandJDBCServer server = null;

    public ByteArrayObjectDataStore(String _dataLocation, boolean _shouldSortFields) {
        super(_dataLocation,_shouldSortFields,EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY);
        server = new DataSandJDBCServer(this);
    }

    public void deleteDatabase() {
        File f = new File(this.dataLocation);
        deleteDirectory(f);
    }

    public String getDataLocation(){
        return this.dataLocation;
    }

    public static void deleteDirectory(File dir) {
        File files[] = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    deleteDirectory(file);
                else
                    file.delete();
            }
        }
        dir.delete();
    }

    public void init() {
        AttributeDescriptor.IS_SERVER_SIDE = true;
        File dbDir = new File(this.dataLocation);
        if (dbDir.exists()) {
            File xDirs[] = dbDir.listFiles();
            if (xDirs != null) {
                for (File xDir : xDirs) {
                    int xID = xDir.getName().indexOf("X-");
                    if (xID != -1) {
                        int xx = Integer.parseInt(xDir.getName().substring(
                                xID + 2));
                        if (X_VECTOR < xx)
                            X_VECTOR = xx;
                        File yDirs[] = xDir.listFiles();
                        if (yDirs != null) {
                            for (File yDir : yDirs) {
                                int yID = yDir.getName().indexOf("Y-");
                                if (yID != -1) {
                                    int yy = Integer.parseInt(yDir.getName()
                                            .substring(yID + 2));
                                    if (Y_VECTOR < yy)
                                        Y_VECTOR = yy;
                                    File zDirs[] = yDir.listFiles();
                                    if (zDirs != null) {
                                        for (File zDir : zDirs) {
                                            int zID = zDir.getName().indexOf(
                                                    "Z-");
                                            if (zID != -1) {
                                                int zz = Integer.parseInt(zDir
                                                        .getName().substring(
                                                                zID + 2));
                                                if (Z_VECTOR < zz)
                                                    Z_VECTOR = zz;
                                                File objects[] = zDir
                                                        .listFiles();
                                                if (objects != null) {
                                                    for (File obj : objects) {
                                                        Shard newLoc = new Shard(xx,yy,zz,Shard.readBlockKey(obj.getPath()),this);
                                                        this.location.put(newLoc.getBlockKey(),newLoc);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }else{
            dbDir.mkdirs();
        }
    }

    public DataSandJDBCResultSet executeSql(String sql,boolean execute){
        DataSandJDBCResultSet rs = new DataSandJDBCResultSet(sql);
        try{
            DataSandJDBCServer.execute(rs, this,execute);
            return rs;
        }catch(Exception err){
            err.printStackTrace();
        }
        return null;
    }

    public void executeSql(String sql, PrintStream out, boolean toCsv) {
        DataSandJDBCResultSet rs = new DataSandJDBCResultSet(sql);
        try {
            int count = 0;
            DataSandJDBCServer.execute(rs, this,true);
            boolean isFirst = true;
            int loc = rs.getFieldsInQuery().size() - 1;
            int totalWidth = 0;
            if(this.shouldSortFields){
                rs.sortFieldsInQuery();
            }
            for (AttributeDescriptor c : rs.getFieldsInQuery()) {
                if (isFirst) {
                    isFirst = false;
                    if (toCsv) {
                        out.print("\"");
                    }
                }

                if (!toCsv) {
                    out.print("|");
                }

                out.print(c.getColumnName());

                if (!toCsv) {
                    int cw = c.getCharWidth();
                    int cnw = c.getColumnName().length();
                    if (cnw > cw) {
                        c.setCharWidth(cnw);
                    }
                    int gap = cw - cnw;
                    for (int i = 0; i < gap; i++) {
                        out.print(" ");
                    }
                }

                totalWidth += c.getCharWidth() + 1;

                if (loc > 0) {
                    if (toCsv) {
                        out.print("\",\"");
                    }
                }
                loc--;
            }

            if (toCsv) {
                out.println("\"");
            } else {
                totalWidth++;
                out.println("|");
                for (int i = 0; i < totalWidth; i++) {
                    out.print("-");
                }
                out.println();
            }

            while (rs.next()) {
                isFirst = true;
                loc = rs.getFieldsInQuery().size() - 1;
                for (AttributeDescriptor c : rs.getFieldsInQuery()) {
                    if (isFirst) {
                        isFirst = false;
                        if (toCsv) {
                            out.print("\"");
                        }
                    }

                    if (!toCsv) {
                        out.print("|");
                    }

                    Object sValue = rs.getObject(c.toString());
                    if (sValue == null) {
                        sValue = "";
                    }
                    if(sValue instanceof byte[]){
                        byte[] data = (byte[])sValue;
                        sValue = "[";
                        for(int i=0;i<data.length;i++){
                            sValue= sValue.toString()+data[i];
                        }
                        sValue = sValue+"]";
                    }
                    out.print(sValue);

                    int cw = c.getCharWidth();
                    int vw = sValue.toString().length();
                    int gap = cw - vw;
                    for (int i = 0; i < gap; i++) {
                        out.print(" ");
                    }

                    if (loc > 0) {
                        if (toCsv) {
                            out.print("\",\"");
                        }
                    }
                    loc--;
                }
                if (toCsv) {
                    out.println("\"");
                } else {
                    out.println("|");
                }
                count++;
            }
            out.println("Total Number Of Records=" + count);
        } catch (Exception err) {
            err.printStackTrace(out);
        }
    }

    public static class NETask implements Runnable {

        private DataSandJDBCResultSet rs = null;
        private TypeDescriptor mainTable = null;
        private ByteArrayObjectDataStore db = null;

        public NETask(DataSandJDBCResultSet _rs, TypeDescriptor _main, ByteArrayObjectDataStore _db) {
            this.rs = _rs;
            this.mainTable = _main;
            this.db = _db;
        }

        public void run() {
            for (int i = rs.fromIndex; i < rs.toIndex; i++) {
                ObjectWithInfo recInfo = null;
                if(rs.getCollectedDataType()==DataSandJDBCResultSet.COLLECT_TYPE_RECORDS){
                    recInfo = db.readNoChildrenWithLocation(mainTable, i);
                }else{
                    recInfo = db.readWithLocation(mainTable, i);
                }
                if(recInfo==null){
                    break;
                }
                rs.addRecords(recInfo, true);
            }
            synchronized (rs) {
                rs.numberOfTasks--;
                if (rs.numberOfTasks == 0) {
                    rs.setFinished(true);
                    rs.notifyAll();
                }
            }
        }
    }

    public void execute(DataSandJDBCResultSet rs) {
        TypeDescriptor table = rs.getMainTable();
        NETask task = new NETask(rs, table, this);
        rs.numberOfTasks = 1;
        threadpool.addTask(task);
    }

    public void commit() {
        for (Shard bl : this.location.values()) {
            bl.save();
        }
    }

    public void close() {
        this.server.close();
        this.commit();
        this.closed = true;
        for (Shard bl : this.location.values()) {
            bl.close();
        }
        this.server.close();
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public Object getEncodedKeyObject(Object dataKey) {
        TypeDescriptor td = this.getTypeDescriptorsContainer().getTypeDescriptorByObject(dataKey);
        ByteArrayEncodeDataContainer ba = (ByteArrayEncodeDataContainer)EncodeDataContainerFactory.newContainer(null,null,this.getEncoderType(),td);
        td.getSerializer().encode(dataKey, ba);
        MD5Identifier md5 = MD5Identifier.createX(ba.getData());
        return md5;
    }
}
