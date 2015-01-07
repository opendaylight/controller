/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.jdbc;

import java.util.Map;

import org.opendaylight.datasand.agents.Message;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class DataSandJDBCMessage extends Message {

    public static final int TYPE_EXECUTE_QUERY = 1;
    public static final int TYPE_QUERY_REPLY = 2;
    public static final int TYPE_QUERY_RECORD = 3;
    public static final int TYPE_QUERY_FINISH = 4;
    public static final int TYPE_QUERY_ERROR = 5;
    public static final int TYPE_METADATA = 6;
    public static final int TYPE_METADATA_REPLY = 7;
    public static final int TYPE_HELLO_GROUP = 8;
    public static final int TYPE_DELEGATE_QUERY = 9;
    public static final int TYPE_DELEGATE_QUERY_RECORD = 10;
    public static final int TYPE_DELEGATE_QUERY_FINISH = 11;
    public static final int TYPE_DELEGATE_WAITING = 12;
    public static final int TYPE_DELEGATE_CONTINUE = 13;
    public static final int TYPE_NODE_WAITING_MARK = 14;
    public static final int TYPE_NODE_WAITING_MARK_REPLY = 15;

    public DataSandJDBCMessage() {
        super();
    }

    public DataSandJDBCMessage(long id,int type,Object data){
        super(id,type,data);
    }

    public DataSandJDBCMessage(Exception _err, int _RSID) {
        super(TYPE_QUERY_ERROR,new DataSandJDBCDataContainer(_err, _RSID));
    }

    public DataSandJDBCMessage(TypeDescriptorsContainer bl) {
        super(TYPE_METADATA_REPLY,new DataSandJDBCDataContainer(bl,-1));
    }

    public DataSandJDBCMessage(DataSandJDBCResultSet _rs,int temp) {
        super(TYPE_QUERY_REPLY,new DataSandJDBCDataContainer(_rs,_rs.getID()));
    }

    public DataSandJDBCMessage(DataSandJDBCResultSet _rs) {
        super(TYPE_EXECUTE_QUERY,new DataSandJDBCDataContainer(_rs,_rs.getID()));
    }

    public DataSandJDBCMessage(DataSandJDBCResultSet _rs,int temp,int temp2) {
        super(TYPE_DELEGATE_QUERY,new DataSandJDBCDataContainer(_rs,_rs.getID()));
    }

    public DataSandJDBCMessage(Map _record, int _rsID,int temp) {
        super(TYPE_DELEGATE_QUERY_RECORD,new DataSandJDBCDataContainer(_record,_rsID));
    }

    public DataSandJDBCMessage(Map _record, int _rsID) {
        super(TYPE_QUERY_RECORD,new DataSandJDBCDataContainer(_record,_rsID));
    }

    public DataSandJDBCMessage(int _rsID) {
        super(TYPE_QUERY_FINISH,new DataSandJDBCDataContainer(null,_rsID));
    }

    public DataSandJDBCMessage(int _rsID,int temp,int temp2) {
        super(TYPE_DELEGATE_QUERY_FINISH,new DataSandJDBCDataContainer(null,_rsID));
    }

    public DataSandJDBCMessage(int _temp,int _temp2){
        super(TYPE_METADATA,new DataSandJDBCDataContainer(null,-1));
    }

    public DataSandJDBCMessage(NetworkID netID,int rsID){
        super(TYPE_DELEGATE_WAITING,new DataSandJDBCDataContainer(netID,rsID));
    }

    public DataSandJDBCMessage(NetworkID netID,int rsID,int temp){
        super(TYPE_NODE_WAITING_MARK,new DataSandJDBCDataContainer(netID,rsID));
    }

    public DataSandJDBCMessage(NetworkID netID,int rsID,int temp,int temp1){
        super(TYPE_NODE_WAITING_MARK_REPLY,new DataSandJDBCDataContainer(netID,rsID));
    }

    public DataSandJDBCMessage(NetworkID netID,int rsID,int temp,int temp1,int temp2){
        super(TYPE_DELEGATE_CONTINUE,new DataSandJDBCDataContainer(netID,rsID));
    }

    public DataSandJDBCResultSet getRS() {
        return (DataSandJDBCResultSet)((DataSandJDBCDataContainer)this.getMessageData()).getData();
    }

    public Map getRecord() {
        return (Map)((DataSandJDBCDataContainer)this.getMessageData()).getData();
    }

    public int getRSID() {
        return ((DataSandJDBCDataContainer)this.getMessageData()).getRsID();
    }

    public Exception getERROR() {
        return (Exception)((DataSandJDBCDataContainer)this.getMessageData()).getData();
    }

    public DataSandJDBCMetaData getMetaData(){
        return (DataSandJDBCMetaData)((DataSandJDBCDataContainer)this.getMessageData()).getData();
    }
    public NetworkID getWaiting(){
        return (NetworkID)((DataSandJDBCDataContainer)this.getMessageData()).getData();
    }
    @Override
    public void encode(Object value, EncodeDataContainer edc) {
        // TODO Auto-generated method stub
        super.encode(value, edc);
    }

    @Override
    public Object decode(EncodeDataContainer edc, int length) {
        Message m = (Message)super.decode(edc, length);
        DataSandJDBCMessage m2 = new DataSandJDBCMessage(m.getMessageID(),m.getMessageType(),m.getMessageData());
        return m2;
    }
}
