/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.jdbc;

import java.net.InetAddress;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.opendaylight.datasand.agents.AutonomousAgent;
import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.agents.Message;
import org.opendaylight.datasand.agents.MessageEntry;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.store.ObjectDataStore;
import org.opendaylight.datasand.store.jdbc.DataSandJDBCResultSet.RSID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class DataSandJDBCConnection extends AutonomousAgent implements Connection {
    private DataSandJDBCMetaData metaData = null;
    private ObjectDataStore dataDataStore;
    private NetworkID destination = null;
    private Map<RSID,QueryContainer> queries = new HashMap<RSID,QueryContainer>();
    private Map<RSID,QueryUpdater> updaters = new HashMap<RSID,QueryUpdater>();
    private Message repetitve = new Message(-1,null);

    public DataSandJDBCConnection(AutonomousAgentManager manager,ObjectDataStore _dataStore) {
        super(107,manager);
        this.dataDataStore = _dataStore;
        this.setARPGroup(107);
        this.sendARP(DataSandJDBCMessage.TYPE_HELLO_GROUP);
        this.registerRepetitiveMessage(10000, 10000, 0,repetitve);
    }

    public DataSandJDBCConnection(AutonomousAgentManager manager,String addr) {
        super(107,manager);
        try{
            if(manager.getNetworkNode().getLocalHost().getPort()==50000){
                destination = NetworkID.valueOf(InetAddress.getByName(addr).getHostAddress() + ":50000:107");
                manager.getNetworkNode().joinNetworkAsSingle(addr);
            }else{
                destination = NetworkID.valueOf(this.getAgentManager().getNetworkNode().getLocalHost().getIPv4AddressAsString()+":50000:107");
            }
        }catch(Exception err){
            err.printStackTrace();
        }
    }
    public Connection getProxy() {
        return this;
        /*
        return (Connection) Proxy.newProxyInstance(this.getClass()
                .getClassLoader(), new Class[] { Connection.class },
                new JDBCProxy(this));
                */
    }

    @Override
    public void processDestinationUnreachable(Message message,NetworkID unreachableSource) {
        System.out.println("Destination Unreachable:"+message.getMessageType()+":"+unreachableSource);
        try{
            throw new Exception("EX");
        }catch(Exception err){
            err.printStackTrace();
        }
    }
    public void sendToDestination(DataSandJDBCMessage m){
        this.send(m, destination);
    }
    @Override
    public void processMessage(Message message, NetworkID source,NetworkID destination) {
        if(message==repetitve){
            sendARP(DataSandJDBCMessage.TYPE_HELLO_GROUP);
            return;
        }
        if(message instanceof DataSandJDBCMessage){
            DataSandJDBCMessage msg = (DataSandJDBCMessage)message;

            switch (msg.getMessageType()) {
            case DataSandJDBCMessage.TYPE_METADATA_REPLY:
                this.metaData = msg.getMetaData();
                synchronized (this) {
                    this.notifyAll();
                }
                break;
            case DataSandJDBCMessage.TYPE_METADATA:
                send(new DataSandJDBCMessage(this.dataDataStore.getTypeDescriptorsContainer()),source);
                break;
            case DataSandJDBCMessage.TYPE_DELEGATE_QUERY:
                try{
                    System.out.println("Starting to execute Query-"+getAgentID()+" From aggregator="+source);
                    DataSandJDBCServer.execute(msg.getRS(), this.dataDataStore,true);
                    QueryUpdater u = new QueryUpdater(msg.getRS(),source);
                    updaters.put(msg.getRS().getRSID(), u);
                    new Thread(u).start();
                }catch (Exception err) {
                    send(new DataSandJDBCMessage(err, msg.getRSID()),source);
                }
                break;
            case DataSandJDBCMessage.TYPE_EXECUTE_QUERY:
                System.out.println("Execute Query:"+getAgentID());
                try {
                    QueryContainer qc = new QueryContainer(source, msg.getRS());
                    this.queries.put(msg.getRSID(),qc);
                    DataSandJDBCServer.execute(msg.getRS(), this.dataDataStore,false);
                    send(new DataSandJDBCMessage(msg.getRS(),0),source);
                } catch (Exception err) {
                    send(new DataSandJDBCMessage(err, msg.getRSID()),source);
                }
                break;
            case DataSandJDBCMessage.TYPE_QUERY_REPLY:
                DataSandJDBCResultSet rs1 = DataSandJDBCStatement.getQuery(msg.getRS().getRSID());
                rs1.updateData(msg.getRS());
                break;
            case DataSandJDBCMessage.TYPE_DELEGATE_QUERY_RECORD:
            {
                QueryContainer c = queries.get(msg.getRSID());
                DataSandJDBCMessage m = new DataSandJDBCMessage(msg.getRecord(),msg.getRSID());
                this.send(m, c.source);
            }
                break;
            case DataSandJDBCMessage.TYPE_QUERY_RECORD:
                DataSandJDBCResultSet rs2 = DataSandJDBCStatement.getQuery(msg.getRSID());
                rs2.addRecord(msg.getRecord(),false);
                break;
            case DataSandJDBCMessage.TYPE_QUERY_FINISH:
                DataSandJDBCResultSet rs3 = DataSandJDBCStatement.removeQuery(msg.getRSID());
                rs3.setFinished(true);
                break;
            case DataSandJDBCMessage.TYPE_DELEGATE_QUERY_FINISH:
            {
                System.out.println("Finished Query "+getAgentID());
                QueryContainer c = queries.get(msg.getRSID());
                MessageEntry e = this.getJournalEntry(c.msg);
                e.removePeer(source);
                if(e.isFinished()){
                    DataSandJDBCMessage end = new DataSandJDBCMessage(msg.getRSID());
                    send(end,c.source);
                }
            }
                break;
            case DataSandJDBCMessage.TYPE_QUERY_ERROR:
                System.err.println("ERROR Executing Query\n");
                msg.getERROR().printStackTrace();
                DataSandJDBCResultSet rs4 = DataSandJDBCStatement.removeQuery(msg.getRSID());
                rs4.setError(msg.getERROR());
                rs4.setFinished(true);
                synchronized (rs4) {
                    rs4.notifyAll();
                }
            case DataSandJDBCMessage.TYPE_DELEGATE_WAITING:
                QueryContainer qc = queries.get(msg.getRSID());
                send(new DataSandJDBCMessage(msg.getWaiting(),msg.getRSID(),0),qc.source);
                break;
            case DataSandJDBCMessage.TYPE_NODE_WAITING_MARK:
                send(new DataSandJDBCMessage(msg.getWaiting(),msg.getRSID(),0,0),source);
                break;
            case DataSandJDBCMessage.TYPE_NODE_WAITING_MARK_REPLY:
                send(new DataSandJDBCMessage(msg.getWaiting(),msg.getRSID(),0,0,0),msg.getWaiting());
                break;
            case DataSandJDBCMessage.TYPE_DELEGATE_CONTINUE:
                QueryUpdater u = updaters.get(msg.getRSID());
                synchronized(u.waitingObject){
                    u.waitingObject.notifyAll();
                }
                break;
            }
        }else
        if(message.getMessageType()==DataSandJDBCMessage.TYPE_HELLO_GROUP){
            getPeerEntry(source);
        }
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        return "Data Sand JDBC Connection";
    }

    private class QueryContainer {
        private Map<NetworkID,Boolean> destToFinish = new HashMap<NetworkID,Boolean>();
        private NetworkID source = null;
        private Message msg = null;
        public QueryContainer(NetworkID _source,DataSandJDBCResultSet _rs){
            this.source = _source;
            msg = new DataSandJDBCMessage(_rs,0,0);
            sendARP(msg);
            addARPJournal(msg,true);
        }
    }

    private class QueryUpdater implements Runnable {

        private DataSandJDBCResultSet rs = null;
        private NetworkID source = null;
        private Object waitingObject = new Object();

        public QueryUpdater(DataSandJDBCResultSet _rs,NetworkID _source) {
            this.rs = _rs;
            this.source = _source;
        }

        public void run() {
            int count = 0;
            while (rs.next()) {
                DataSandJDBCMessage rec = new DataSandJDBCMessage(rs.getCurrent(), rs.getRSID(),0);
                send(rec,source);
                count++;
                if(count>=DataSandJDBCResultSet.RECORD_Threshold_Release){
                    synchronized(waitingObject){
                        DataSandJDBCMessage m = new DataSandJDBCMessage(getAgentID(),rs.getRSID());
                        send(m,source);
                        try{waitingObject.wait();}catch(Exception err){err.printStackTrace();}
                        count = 0;
                    }
                }
            }
            updaters.remove(rs.getRSID());
            DataSandJDBCMessage end = new DataSandJDBCMessage(rs.getRSID(),0,0);
            send(end,source);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() throws SQLException {
        this.getAgentManager().shutdown();
    }

    @Override
    public void commit() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new DataSandJDBCStatement(this).getProxy();
    }

    @Override
    public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return new DataSandJDBCStatement(this).getProxy();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return new DataSandJDBCStatement(this).getProxy();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCatalog() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (this.metaData == null) {
            DataSandJDBCMessage cmd = new DataSandJDBCMessage(-1,-1);
            synchronized (this) {
                send(cmd,destination);
                try {
                    this.wait();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
        return metaData;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        System.err.println("SQL 1=" + sql);
        return new DataSandJDBCStatement(this, sql).getProxy();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        System.err.println("SQL 2=" + sql);
        return new DataSandJDBCStatement(this, sql).getProxy();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        System.err.println("SQL 3=" + sql);
        return new DataSandJDBCStatement(this, sql).getProxy();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        System.err.println("SQL 4=" + sql);
        return new DataSandJDBCStatement(this, sql).getProxy();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        System.err.println("SQL 5=" + sql);
        return new DataSandJDBCStatement(this, sql).getProxy();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        System.err.println("SQL 6=" + sql);
        return new DataSandJDBCStatement(this, sql).getProxy();
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void rollback() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setClientInfo(String name, String value)
            throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSchema(String schema) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getSchema() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

}
