package org.opendaylight.controller.md.sal.dom.xsql.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrint;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrintNode;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLColumn;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLCriteria;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLODLUtils;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

public class JDBCResultSet implements Serializable, ResultSet,
        ResultSetMetaData {
    private static final long serialVersionUID = -7450200738431047057L;

    private String sql = null;
    private List<XSQLBluePrintNode> tablesInQuery = new ArrayList<XSQLBluePrintNode>();
    private Map<String, XSQLBluePrintNode> tablesInQueryMap = new ConcurrentHashMap<String, XSQLBluePrintNode>();
    private List<XSQLColumn> fieldsInQuery = new ArrayList<XSQLColumn>();
    private transient LinkedList<Map<String, Object>> records = new LinkedList<>();
    private transient Map<String, Object> currentRecord = null;
    private boolean finished = false;
    private int id = 0;
    private static Integer nextID = new Integer(0);
    public int numberOfTasks = 0;
    private Map<String, Map<XSQLColumn, List<XSQLCriteria>>> criteria = new ConcurrentHashMap<String, Map<XSQLColumn, List<XSQLCriteria>>>();
    private Exception err = null;
    private List<Record> EMPTY_RESULT = new LinkedList<Record>();
    private transient Map<String,JDBCResultSet> subQueries = new HashMap<String,JDBCResultSet>();

    public ResultSet getProxy() {
         return (ResultSet) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {ResultSet.class }, new JDBCProxy(this));
    }

    public void setSQL(String _sql) {
        this.sql = _sql;
    }

    public JDBCResultSet addSubQuery(String _sql,String logicalName) {
        if (subQueries == null) {
            subQueries = new HashMap<String,JDBCResultSet>();
        }
        JDBCResultSet rs = new JDBCResultSet(_sql);
        this.subQueries.put(logicalName,rs);
        return rs;
    }

    public Map<String,JDBCResultSet> getSubQueries() {
        if (this.subQueries==null) {
            this.subQueries = new HashMap<>();
        }
        return this.subQueries;
    }

    public JDBCResultSet(String _sql) {
        synchronized (JDBCResultSet.class) {
            nextID++;
            id = nextID;
        }
        this.sql = _sql;
    }

    public String getSQL() {
        return this.sql;
    }

    public void setError(Exception _err) {
        this.err = _err;
    }

    public Exception getError() {
        return this.err;
    }

    public void updateData(JDBCResultSet rs) {
        synchronized (this) {
            this.tablesInQuery = rs.tablesInQuery;
            this.tablesInQueryMap = rs.tablesInQueryMap;
            this.fieldsInQuery = rs.fieldsInQuery;
            this.notifyAll();
        }
    }

    public int isObjectFitCriteria(Map<String, Object> objValues, String tableName) {
        Map<XSQLColumn, List<XSQLCriteria>> tblCriteria = criteria
                .get(tableName);
        if (tblCriteria == null) {
            return 1;
        }
        for (Map.Entry<XSQLColumn, List<XSQLCriteria>> cc : tblCriteria
                .entrySet()) {
            for (XSQLCriteria c : cc.getValue()) {
                Object value = objValues.get(cc.getKey().toString());
                int result = c.checkValue(value);
                if (result == 0) {
                    return 0;
                }
            }
        }
        return 1;
    }

    public int isObjectFitCriteria(Object element, Class<?> cls) {
        Map<XSQLColumn, List<XSQLCriteria>> tblCriteria = criteria.get(cls
                .getName());
        if (tblCriteria == null) {
            return 1;
        }
        for (Map.Entry<XSQLColumn, List<XSQLCriteria>> cc : tblCriteria
                .entrySet()) {
            for (XSQLCriteria c : cc.getValue()) {
                int result = c.isObjectFitCriteria(element, cc.getKey()
                        .getName());
                if (result == 0) {
                    return 0;
                }
            }
        }
        return 1;
    }

    public Map<String, Map<XSQLColumn, List<XSQLCriteria>>> getCriteria() {
        return this.criteria;
    }

    public int getID() {
        return this.id;
    }

    public List<XSQLBluePrintNode> getTables() {
        return tablesInQuery;
    }

    public void addTableToQuery(XSQLBluePrintNode node) {
        if (this.tablesInQueryMap.containsKey(node.getBluePrintNodeName())) {
            return;
        }
        this.tablesInQuery.add(node);
        this.tablesInQueryMap.put(node.getBluePrintNodeName(), node);
    }

    public List<XSQLColumn> getFields() {
        return this.fieldsInQuery;
    }

    public XSQLBluePrintNode getMainTable() {
        if (tablesInQuery.size() == 1) {
            return tablesInQuery.get(0);
        }
        XSQLBluePrintNode result = null;
        for (XSQLBluePrintNode node : tablesInQuery) {
            if (result == null) {
                result = node;
            } else if (result.getLevel() < node.getLevel()) {
                result = node;
            }
        }
        return result;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean b) {
        this.finished = b;
    }

    public int size() {
        return this.records.size();
    }

    public void addRecord(Map<String, Object> r) {
        synchronized (this) {
            if (records == null) {
                records = new LinkedList<>();
            }
            records.add(r);
            this.notifyAll();
        }
    }

    public void addRecord(ArrayList<?> hierarchy) {
        Map<String, Object> rec = new HashMap<>();
        for (int i = hierarchy.size() - 1; i >= 0; i--) {
            Object element = hierarchy.get(i);
            for (XSQLColumn c : fieldsInQuery) {
                if (c.getTableName().equals(element.getClass().getSimpleName())) {
                    try {
                        Method m = element.getClass().getMethod(c.getName(),
                                null);
                        Object value = m.invoke(element, null);
                        rec.put(c.getName(), value);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            }
        }
        this.records.add(rec);
    }

    public boolean next() {
        this.currentRecord = null;
        if (records == null) {
            records = new LinkedList<>();
        }
        while (!finished || records.size() > 0) {
            synchronized (this) {
                if (records.size() == 0) {
                    try {
                        this.wait(1000);
                    } catch (Exception err) {
                    }
                    if (records.size() > 0) {
                        try {
                            currentRecord = records.removeFirst();
                            return true;
                        } finally {
                            this.notifyAll();
                        }
                    }
                } else {
                    try {
                        currentRecord = records.removeFirst();
                        return true;
                    } finally {
                        this.notifyAll();
                    }
                }
            }
        }
        return false;
    }

    public Map<String, Object> getCurrent() {
        return this.currentRecord;
    }

    private void createRecord(Object data, XSQLBluePrintNode node) {
        Map<String, Object> rec = new HashMap<>();
        for (XSQLColumn c : this.fieldsInQuery) {
            if (c.getTableName().equals(node.getBluePrintNodeName())) {
                try {
                    Method m = node.getInterface().getMethod(c.getName(), null);
                    Object value = m.invoke(data, null);
                    if (value != null) {
                        rec.put(c.getName(), value);
                    } else {
                        rec.put(c.getName(), "");
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }

            }
        }
    }

    public static class Record {
        public Map<String, Object> data = new HashMap<>();
        public Object element = null;
        public boolean fitCriteria = true;
        public Map<String, Object> getRecord() {
            return this.data;
        }
    }

    public static class RecordsContainer{
        public List<Record> records = new LinkedList<Record>();
        public List<Record> fitRecords = new LinkedList<Record>();
        public Object currentObject = null;
    }

    private void collectColumnValues(RecordsContainer rContainer, XSQLBluePrintNode bpn) {
        Collection<?> subChildren = XSQLODLUtils.getChildrenCollection(rContainer.currentObject);
        Record r = new Record();
        r.element = rContainer.currentObject;
        for (Object stc : subChildren) {
            if (stc.getClass().getName().endsWith("ImmutableUnkeyedListEntryNode")){
                r.fitCriteria = false;
                rContainer.currentObject = stc;
                collectColumnValues(rContainer, bpn);
            }else
            if (stc.getClass().getName().endsWith("ImmutableAugmentationNode")) {
                Map<?, ?> values = XSQLODLUtils.getChildren(stc);
                for (Object key : values.keySet()) {
                    Object val = values.get(key);
                    if (val.getClass().getName().endsWith("ImmutableLeafNode")) {
                        Object value = XSQLODLUtils.getValue(val);
                        String k = XSQLODLUtils.getNodeName(val);
                        if (value != null) {
                            r.data.put(bpn.getBluePrintNodeName() + "." + k,value.toString());
                        }
                    }
                }
            } else if (stc.getClass().getName().endsWith("ImmutableLeafNode")) {
                String k = XSQLODLUtils.getNodeName(stc);
                Object value = XSQLODLUtils.getValue(stc);
                if (value != null) {
                    r.data.put(bpn.getBluePrintNodeName() + "." + k,value.toString());
                }
            }
        }
        if(r.fitCriteria)
            rContainer.records.add(r);
    }

    private void addToData(Record rec, XSQLBluePrintNode bpn,XSQLBluePrint bluePrint, Map<String, Object> fullRecord) {
        XSQLBluePrintNode eNodes[] = bluePrint.getBluePrintNodeByODLTableName(XSQLODLUtils.getNodeIdentiofier(rec.element));
        if (bpn != null) {
            for (XSQLColumn c : fieldsInQuery) {
                for (XSQLBluePrintNode eNode : eNodes) {
                    if (((XSQLBluePrintNode) c.getBluePrintNode())
                            .getBluePrintNodeName().equals(
                                    eNode.getBluePrintNodeName())) {
                        // Object value = Criteria.getValue(rec.element,
                        // c.getName());
                        String columnName = c.toString();
                        Object value = fullRecord.get(columnName);
                        if (value != null) {
                            try {
                                Object rsValue = c.getResultSetValue(value);
                                c.setCharWidth(rsValue.toString().length());
                                rec.data.put(columnName, rsValue);
                            } catch (Exception err) {
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean beenHere(Set<String> beenHereElement, Object element) {
        if (beenHereElement == null) {
            beenHereElement = new HashSet<String>();
        }

        String elementKey = null;

        try {
            elementKey = element.toString();
        } catch (Exception err) {
            elementKey = "Unknown";
        }

        if (beenHereElement.contains(elementKey)) {
            return true;
        }

        beenHereElement.add(elementKey);
        return false;
    }

    public List<Object> getChildren(Object node, String tableName,XSQLBluePrint bluePrint) {

        List<Object> children = XSQLODLUtils.getMChildren(node);
        List<Object> result = new LinkedList<Object>();

        for (Object child : children) {

            String odlNodeName = XSQLODLUtils.getNodeIdentiofier(child);
            if (odlNodeName == null) {
                if(child instanceof DataContainerNode){
                    List<Object> augChidlren = getChildren(child,tableName,bluePrint);
                    result.addAll(augChidlren);
                }
                continue;
            }

            XSQLBluePrintNode eNodes[] = bluePrint
                    .getBluePrintNodeByODLTableName(odlNodeName);
            if (eNodes == null) {
                continue;
            }

            boolean match = false;
            for (XSQLBluePrintNode enode : eNodes) {
                if (tableName.startsWith(enode.toString())) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                continue;
            }

            if(child.getClass().getName().endsWith("ImmutableUnkeyedListNode")){
                result.add(child);
            }else
            if (child.getClass().getName().endsWith("ImmutableContainerNode")) {
                result.add(child);
            }else
            if (child.getClass().getName().endsWith("ImmutableAugmentationNode")) {
                List<Object> _children = XSQLODLUtils.getMChildren(child);
                for (Object c : _children) {
                    if (c.getClass().getName()
                            .endsWith("ImmutableContainerNode")) {
                        result.add(c);
                    }
                }
            }else
            if (child.getClass().getName().endsWith("ImmutableMapNode")) {
                result.addAll(XSQLODLUtils.getMChildren(child));
            }else{
                XSQLAdapter.log("Missed Node Data OF Type="+child.getClass().getName());
            }
        }
        return result;
    }

    public List<Record> addRecords(Object element, XSQLBluePrintNode node,boolean root, String tableName, XSQLBluePrint bluePrint) {
        List<Record> result = new LinkedList<Record>();
        String nodeID = XSQLODLUtils.getNodeIdentiofier(element);
        if (node.getODLTableName().equals(nodeID)) {
            XSQLBluePrintNode bluePrintNode = bluePrint.getBluePrintNodeByODLTableName(nodeID)[0];
            RecordsContainer rContainer = new RecordsContainer();
            rContainer.currentObject = element;
            XSQLBluePrintNode bpn = this.tablesInQueryMap.get(bluePrintNode.getBluePrintNodeName());
            if (this.criteria.containsKey(bluePrintNode.getBluePrintNodeName()) || bpn != null) {
                collectColumnValues(rContainer, bpn);
                for(Record r:rContainer.records){
                    if (!(isObjectFitCriteria(r.data,bpn.getBluePrintNodeName()) == 1)) {
                        r.fitCriteria = false;
                    }
                    if(r.fitCriteria){
                        Record rec = new Record();
                        rec.element = r.element;
                        addToData(rec, bpn, bluePrint, r.data);
                        rContainer.fitRecords.add(rec);
                    }
                }
                if(rContainer.fitRecords.isEmpty())
                    return EMPTY_RESULT;
            }
            if(rContainer.records.isEmpty()){
                Record rec = new Record();
                rec.element = rContainer.currentObject;
                if (root) {
                    addRecord(rec.data);
                } else {
                    result.add(rec);
                }
            }else{
                for(Record rec:rContainer.fitRecords){
                    if (root) {
                        addRecord(rec.data);
                    } else {
                        result.add(rec);
                    }
                }
            }
            return result;
        }

        XSQLBluePrintNode parent = node.getParent();
        List<Record> subRecords = addRecords(element, parent, false, tableName,bluePrint);
        for (Record subRec : subRecords) {
            List<Object> subO = getChildren(subRec.element, tableName,bluePrint);
            if (subO != null) {
                for (Object subData : subO) {
                    RecordsContainer rContainer = new RecordsContainer();
                    rContainer.currentObject = subData;

                    String recID = XSQLODLUtils.getNodeIdentiofier(rContainer.currentObject);
                    XSQLBluePrintNode eNodes[] = bluePrint.getBluePrintNodeByODLTableName(recID);
                    XSQLBluePrintNode bpn = null;
                    for (XSQLBluePrintNode eNode : eNodes) {
                        bpn = this.tablesInQueryMap.get(eNode.getBluePrintNodeName());
                        if (bpn != null) {
                            break;
                        }
                    }
                    if (bpn != null) {
                        collectColumnValues(rContainer, bpn);
                        for(Record r:rContainer.records){
                            if ((isObjectFitCriteria(r.data,bpn.getBluePrintNodeName()) == 1)) {
                                Record rec = new Record();
                                rec.element = r.element;
                                addToData(rec, bpn, bluePrint, r.data);
                            } else {
                                r.fitCriteria = false;
                            }
                        }
                    }
                    if(rContainer.records.isEmpty()){
                        Record rec = new Record();
                        rec.element = rContainer.currentObject;
                        if (root) {
                            if (!rec.data.isEmpty()) {
                                addRecord(rec.data);
                            }
                        } else {
                            result.add(rec);
                        }
                    }else{
                        for(Record r:rContainer.records){
                            if (r.fitCriteria) {
                                if (root) {
                                    if (!r.data.isEmpty()) {
                                        addRecord(r.data);
                                    }
                                } else {
                                    result.add(r);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void afterLast() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeFirst() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearWarnings() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean first() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getConcurrency() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCursorName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFetchSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map)
            throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return currentRecord.get(this.fieldsInQuery.get(columnIndex - 1)
                .toString());
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map)
            throws SQLException {
        return getObject(columnLabel);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return currentRecord.get(columnLabel);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRow() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Statement getStatement() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return "Kuku";
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return "Kuku";
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void insertRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAfterLast() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isFirst() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLast() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean last() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void moveToInsertRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean previous() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void refreshRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean relative(int rows) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x,
            long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream,
            long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBoolean(String columnLabel, boolean x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader,
            int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader,
            long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader,
            long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNString(int columnIndex, String nString)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNString(String columnLabel, String nString)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x)
            throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean wasNull() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return fieldsInQuery.size();
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return this.fieldsInQuery.get(column - 1).toString();
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return 12;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    // //Metadata

}
