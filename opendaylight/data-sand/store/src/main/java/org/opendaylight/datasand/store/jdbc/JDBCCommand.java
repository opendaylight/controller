package org.opendaylight.datasand.store.jdbc;

import java.io.Serializable;
import java.util.Map;

import org.opendaylight.datasand.codec.TypeDescriptorsContainer;

public class JDBCCommand implements Serializable {
    public int type = 0;
    public static final int TYPE_EXECUTE_QUERY = 1;
    public static final int TYPE_QUERY_REPLY = 2;
    public static final int TYPE_QUERY_RECORD = 3;
    public static final int TYPE_QUERY_FINISH = 4;
    public static final int TYPE_QUERY_ERROR = 5;
    public static final int TYPE_METADATA = 6;
    public static final int TYPE_METADATA_REPLY = 7;

    private JDBCResultSet rs = null;
    private Map record = null;
    private int rsID = -1;
    private Exception err = null;
    private byte[] mdsalTableRepositoryData = null;

    public JDBCCommand() {

    }

    public void setType(int t) {
        this.type = t;
    }

    public JDBCCommand(Exception _err, int _RSID) {
        this.type = TYPE_QUERY_ERROR;
        this.err = _err;
        this.rsID = _RSID;
    }

    public JDBCCommand(TypeDescriptorsContainer bl) {
        this.type = TYPE_METADATA_REPLY;
        this.mdsalTableRepositoryData = bl.getMDSALTableRepositoryData();
    }

    public JDBCCommand(JDBCResultSet _rs, int _type) {
        this.type = TYPE_EXECUTE_QUERY;
        this.rs = _rs;
        this.type = _type;
        this.rsID = rs.getID();
    }

    public JDBCCommand(Map _record, int _rsID) {
        this.record = _record;
        this.rsID = _rsID;
        this.type = TYPE_QUERY_RECORD;
    }

    public JDBCCommand(int _rsID) {
        this.rsID = _rsID;
        this.type = TYPE_QUERY_FINISH;
    }

    public int getType() {
        return this.type;
    }

    public JDBCResultSet getRS() {
        return this.rs;
    }

    public Map getRecord() {
        return this.record;
    }

    public int getRSID() {
        return this.rsID;
    }

    public Exception getERROR() {
        return this.err;
    }

    public byte[] getMdsalTableRepositoryData() {
        return mdsalTableRepositoryData;
    }
}
