package org.opendaylight.controller.md.sal.dom.xsql.jdbc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;

public class JDBCConnection extends Thread implements Connection {
    private Socket socket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private LinkedList<byte[]> queue = new LinkedList<byte[]>();
    private XSQLAdapter adapter = null;

    public JDBCConnection(Socket s, XSQLAdapter _a) {
        this.socket = s;
        this.adapter = _a;
        try {
            in = new DataInputStream(
                new BufferedInputStream(s.getInputStream()));
            out = new DataOutputStream(
                new BufferedOutputStream(s.getOutputStream()));
            new JDBCObjectReader();
            this.start();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public JDBCConnection(String addr) throws Exception {
        socket = new Socket(addr, 40004);
        try {
            in = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(
                new BufferedOutputStream(socket.getOutputStream()));
            new JDBCObjectReader();
            this.start();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public JDBCConnection(boolean server) {
        try {
            ServerSocket s = new ServerSocket(50003);
            socket = s.accept();
            try {
                in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
                new JDBCObjectReader();
                this.start();
            } catch (Exception err) {
                err.printStackTrace();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }


    private boolean isStopped() {
        if (adapter != null && adapter.stopped) {
            return true;
        }
        if (socket == null || socket.isClosed()) {
            return true;
        }
        return false;
    }

    public void run() {
        byte data[] = null;
        while (!isStopped()) {
            try {
                int len = in.readInt();
                data = new byte[len];
                in.readFully(data);
                addObject(data);

            } catch (Exception err) {
                System.out.println("Connection Lost or Closed.");
                try {
                    socket.close();
                } catch (Exception err2) {
                }
                //err.printStackTrace();
            }
        }
    }

    private void addObject(byte[] data) {
        synchronized (queue) {
            queue.add(data);
            queue.notifyAll();
        }
    }

    private class JDBCObjectReader extends Thread {

        public JDBCObjectReader() {
            super("JDBCObjectReader");
            start();
        }

        public void run() {
            while (!isStopped()) {
                byte data[] = null;
                synchronized (queue) {
                    if (queue.size() == 0) {
                        try {
                            queue.wait(1000);
                        } catch (Exception err) {
                        }
                    }
                    if (queue.size() > 0) {
                        data = queue.removeFirst();
                    }
                }
                if (data != null) {
                    JDBCCommand command = (JDBCCommand) deSerialize(data);
                    processCommand(command);
                }
            }
        }

        private Object deSerialize(byte data[]) {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                ObjectInputStream oin = new ObjectInputStream(in);
                return oin.readObject();
            } catch (Exception err) {
                err.printStackTrace();
            }
            return null;
        }
    }

    public void processCommand(JDBCCommand cmd) {
        switch (cmd.getType()) {
            case JDBCCommand.TYPE_EXECUTE_QUERY:
                try {
                    JDBCServer.execute(cmd.getRS(), adapter);
                    send(new JDBCCommand(cmd.getRS(),
                        JDBCCommand.TYPE_QUERY_REPLY));
                    QueryUpdater u = new QueryUpdater(cmd.getRS());
                    new Thread(u).start();
                } catch (Exception err) {
                    send(new JDBCCommand(err, cmd.getRSID()));
                }
                break;
            case JDBCCommand.TYPE_QUERY_REPLY:
                JDBCResultSet rs1 = JDBCStatement.getQuery(cmd.getRS().getID());
                rs1.updateData(cmd.getRS());
                break;
            case JDBCCommand.TYPE_QUERY_RECORD:
                JDBCResultSet rs2 = JDBCStatement.getQuery(cmd.getRSID());
                rs2.addRecord(cmd.getRecord());
                break;
            case JDBCCommand.TYPE_QUERY_FINISH:
                JDBCResultSet rs3 = JDBCStatement.removeQuery(cmd.getRSID());
                rs3.setFinished(true);
                break;
            case JDBCCommand.TYPE_QUERY_ERROR:
                System.err.println("ERROR Executing Query\n");
                cmd.getERROR().printStackTrace();
                JDBCResultSet rs4 = JDBCStatement.removeQuery(cmd.getRSID());
                rs4.setError(cmd.getERROR());
                rs4.setFinished(true);
                synchronized (rs4) {
                    rs4.notifyAll();
                }
        }
    }

    private class QueryUpdater implements Runnable {

        private JDBCResultSet rs = null;

        public QueryUpdater(JDBCResultSet _rs) {
            this.rs = _rs;
        }

        public void run() {
            while (rs.next()) {
                JDBCCommand rec = new JDBCCommand(rs.getCurrent(), rs.getID());
                send(rec);
            }
            JDBCCommand end = new JDBCCommand(rs.getID());
            send(end);
        }
    }

    public void send(Object o) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(o);
            byte data[] = bout.toByteArray();
            synchronized (socket) {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
            }
        } catch (Exception err) {
            err.printStackTrace();
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
        try {
            socket.close();
        } catch (Exception err) {
        }
        socket = null;
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
        return new JDBCStatement(this);
    }

    @Override
    public Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Statement createStatement(int resultSetType,
        int resultSetConcurrency)
        throws SQLException {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        // TODO Auto-generated method stub
        return null;
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

