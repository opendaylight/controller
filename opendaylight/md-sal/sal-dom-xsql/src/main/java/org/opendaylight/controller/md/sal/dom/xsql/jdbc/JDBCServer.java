package org.opendaylight.controller.md.sal.dom.xsql.jdbc;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrint;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrintNode;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLColumn;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLCriteria;

public class JDBCServer extends Thread {
    private ServerSocket socket = null;
    private XSQLAdapter adapter = null;

    public JDBCServer(XSQLAdapter a) {
        super("JDBC Server");
        this.adapter = a;
        start();
    }

    public void run() {
        try {
            socket = new ServerSocket(40004);
            while (!adapter.stopped) {
                Socket s = socket.accept();
                new JDBCConnection(s, adapter);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public void connectToClient(String addr) {
        try {
            Socket s = new Socket(addr, 50003);
            new JDBCConnection(s, adapter);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void execute(JDBCResultSet rs, XSQLAdapter adapter)throws SQLException {
        if(rs.getSQL().toLowerCase().trim().equals("select 1")){
            rs.setFinished(true);
            return;
        }
        checkAndBreakSubQueries(rs, adapter);
        if (rs.getSubQueries().size() == 0) {
            parseTables(rs, adapter.getBluePrint());
            parseFields(rs, adapter.getBluePrint());
            parseCriteria(rs, adapter.getBluePrint());
            try {
                adapter.execute(rs);
            } catch (Exception err) {
                throw new SQLException("Error", err);
            }
        } else {
            parseExternalQuery(rs);
        }
    }

    public static void parseExternalQuery(JDBCResultSet rs) throws SQLException {
        String sql = rs.getSQL();
        for (Map.Entry<String, JDBCResultSet> entry : rs.getSubQueries()
                .entrySet()) {
            int index = sql.toLowerCase().indexOf(entry.getValue().getSQL());
            String extSql = sql.substring(0, index);
            index = extSql.lastIndexOf("(");
            extSql = extSql.substring(0, index);
            System.out.println("External SQL=" + extSql);
            parseLogicalFields(extSql, rs);
        }
    }

    public static void parseLogicalFields(String sql, JDBCResultSet rs)
            throws SQLException {
        if(sql.trim().toLowerCase().equals("select * from")){
            for (Map.Entry<String, JDBCResultSet> entry : rs.getSubQueries().entrySet()) {
                for(XSQLBluePrintNode node:entry.getValue().getTables()){
                    rs.addTableToQuery(node);
                }
                rs.getFields().addAll(entry.getValue().getFields());
                while (entry.getValue().next()) {
                    Map<String, Object> rec = entry.getValue().getCurrent();
                    Map<String, Object> newRec = new HashMap<>();
                    newRec.putAll(rec);
                    rs.addRecord(newRec);
                }
            }
            rs.setFinished(true);
            return;
        }

        Map<String, XSQLBluePrintNode> logicalNameToNode = new HashMap<String, XSQLBluePrintNode>();
        Map<String, String> origNameToName = new HashMap<String, String>();
        List<XSQLColumn> columnOrder = new ArrayList<>();
        int nextLogField = addNextLogicalField(sql, 0,
                logicalNameToNode, origNameToName,columnOrder);
        int next = sql.toLowerCase().indexOf(" as ", nextLogField);
        while (next != -1) {
            nextLogField = addNextLogicalField(sql, nextLogField + 1,
                    logicalNameToNode, origNameToName,columnOrder);
            next = sql.toLowerCase().indexOf(" as ", nextLogField + 1);
        }

        for (XSQLBluePrintNode node : logicalNameToNode.values()) {
            rs.addTableToQuery(node);
        }
        rs.getFields().addAll(columnOrder);
        for (Map.Entry<String, JDBCResultSet> entry : rs.getSubQueries().entrySet()) {
            while (entry.getValue().next()) {
                Map<String, Object> rec = entry.getValue().getCurrent();
                Map<String, Object> newRec = new HashMap<>();
                for (Map.Entry<String, Object> e : rec.entrySet()) {
                    Object value = e.getValue();
                    String logicalKey = origNameToName.get(e.getKey());
                    if (value != null && logicalKey != null) {
                        newRec.put(logicalKey, value);
                    }
                }
                rs.addRecord(newRec);
            }
        }
        rs.setFinished(true);
    }

    public static void main(String args[]) {
        String sql = "SELECT DISTINCT"
                + "\"LOGICAL_TABLE_1\".\"nodes/node.id\" AS \"COL0\"\n"
                + ",\"LOGICAL_TABLE_1\".\"nodes/node.id\" AS \"COL1\"\n"
                + ",\"LOGICAL_TABLE_1\".\"nodes/node.id\" AS \"COL2\"\n"
                + "FROM\n"
                + "(select * from nodes/node;) \"LOGICAL_TABLE_1\"\n";
        JDBCResultSet rs = new JDBCResultSet(sql);
        try {
            parseLogicalFields(sql, rs);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static int addNextLogicalField(String sql, int startIndex,
            Map<String, XSQLBluePrintNode> logicalNameToNode,
            Map<String, String> origNameToName, List<XSQLColumn> columnOrder) {
        int index1 = sql.indexOf("\"", startIndex);
        int index2 = sql.indexOf("\".\"", index1);
        int index3 = sql.indexOf("\"", index2 + 3);
        int index4 = sql.toLowerCase().indexOf(" as ", startIndex);
        int index5 = sql.indexOf("\"", index4);
        int index6 = sql.indexOf("\"", index5 + 1);

        String tblName = sql.substring(index1 + 1, index2);
        String origFieldNameFull = sql.substring(index2 + 3, index3);
        String origTableName = "";
        String origFieldName = "";
        if (origFieldNameFull.indexOf(".") != -1) {
            origTableName = origFieldNameFull.substring(0,origFieldNameFull.indexOf("."));
            origFieldName = origFieldNameFull.substring(origFieldNameFull.indexOf(".") + 1);
        }
        String logicalFieldName = sql.substring(index5 + 1, index6);
        XSQLBluePrintNode node = logicalNameToNode.get(tblName);
        if (node == null) {
            node = new XSQLBluePrintNode(tblName, origTableName, 0);
            logicalNameToNode.put(tblName, node);
        }
        columnOrder.add(node.addColumn(logicalFieldName, tblName, origFieldName, origTableName));
        origNameToName.put(origFieldNameFull, tblName + "." + logicalFieldName);
        return index6;
    }

    public static void checkAndBreakSubQueries(JDBCResultSet rs,XSQLAdapter adapter) throws SQLException {
        String sql = rs.getSQL().toLowerCase();
        int index = sql.indexOf("select");
        if (index == -1)
            throw new SQLException("Select statement is missing...");
        int index2 = sql.indexOf("select", index + 6);
        if (index2 != -1) {
            int startSubQuery = index2;
            for (int i = startSubQuery; i >= 0; i--) {
                if (sql.charAt(i) == '(') {
                    startSubQuery = i;
                    break;
                }
            }
            int braketCount = 0;
            int endSubQuery = startSubQuery;
            do {
                if (sql.charAt(endSubQuery) == '(') {
                    braketCount++;
                }
                else if (sql.charAt(endSubQuery) == ')') {
                    braketCount--;
                }
                endSubQuery++;
            } while (braketCount > 0 || endSubQuery == sql.length());
            String subQuerySQL = sql.substring(startSubQuery + 1,endSubQuery - 1);
            if(rs.getSQL().toLowerCase().substring(0,startSubQuery).trim().equals("select * from")){
                rs.setSQL(subQuerySQL);
                return;
            }
            index = sql.indexOf("\"", endSubQuery);
            index2 = sql.indexOf("\"", index + 1);
            if(index==-1){
                index = endSubQuery;
                index2 = sql.length();
            }
            String logicalName = rs.getSQL().substring(index + 1, index2).trim();
            JDBCResultSet subRS = rs.addSubQuery(subQuerySQL, logicalName);
            JDBCServer.execute(subRS, adapter);
        }
    }

    public static void parseTables(JDBCResultSet rs, XSQLBluePrint bp)
            throws SQLException {
        String lowSQL = rs.getSQL().toLowerCase();
        int from = lowSQL.indexOf("from");
        int where = lowSQL.indexOf("where");
        int subQuery = lowSQL.indexOf("select", 2);
        int fromTo = lowSQL.indexOf(";");

        if (where != -1 && subQuery != -1 && where < subQuery) {
            fromTo = where;
        } else if (where != -1 && subQuery != -1 && where > subQuery) {
            fromTo = subQuery;
        } else if (where != -1) {
            fromTo = where;
        } else if (subQuery != -1) {
            fromTo = subQuery;
        }

        if (from == -1) {
            throw new SQLException("Missing \"from\" statement.");
        }

        if (fromTo == -1) {
            throw new SQLException("Missing terminating \";\".");
        }

        String tableNames = rs.getSQL().substring(from + 4, fromTo).trim();
        StringTokenizer tokens = new StringTokenizer(tableNames, ",");
        while (tokens.hasMoreTokens()) {
            String tableName = tokens.nextToken().trim();
            XSQLBluePrintNode table = bp.getBluePrintNodeByTableName(tableName);
            if (table == null) {
                throw new SQLException("Unknown table name \"" + tableName
                        + "\"");
            }
            rs.addTableToQuery(table);
        }
    }

    public static void addCriteria(XSQLColumn col, XSQLCriteria c,
            JDBCResultSet rs) {
        Map<XSQLColumn, List<XSQLCriteria>> tblCriteria = rs.getCriteria().get(
                col.getTableName());
        if (tblCriteria == null) {
            tblCriteria = new ConcurrentHashMap<XSQLColumn, List<XSQLCriteria>>();
            rs.getCriteria().put(col.getTableName(), tblCriteria);
        }
        List<XSQLCriteria> lstCriteria = tblCriteria.get(col);
        if (lstCriteria == null) {
            lstCriteria = new ArrayList<XSQLCriteria>();
            tblCriteria.put(col, lstCriteria);
        }
        lstCriteria.add(c);
    }

    public static void parseFields(JDBCResultSet rs, XSQLBluePrint bp)
            throws SQLException {
        String lowSQL = rs.getSQL().toLowerCase();
        if (!lowSQL.startsWith("select")) {
            throw new SQLException("Missing 'select' statement.");
        }
        int from = lowSQL.indexOf("from");
        if (from == -1) {
            throw new SQLException("Missing 'from' statement.");
        }
        String fields = rs.getSQL().substring(6, from).trim();
        StringTokenizer tokens = new StringTokenizer(fields, ",");
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            if (token.equals("*")) {
                for (XSQLBluePrintNode table : rs.getTables()) {
                    rs.getFields().addAll(table.getColumns());
                }
                return;
            }
            if (token.indexOf(".") != -1) {
                XSQLBluePrintNode tbl = bp.getBluePrintNodeByTableName(token
                        .substring(0, token.indexOf(".")).trim());
                String p = token.substring(token.indexOf(".") + 1);
                if (p.equals("*")) {
                    for (XSQLColumn c : tbl.getColumns()) {
                        rs.getFields().add(c);
                    }
                } else {
                    XSQLColumn col = tbl.findColumnByName(p);
                    rs.getFields().add(col);
                }
            } else {
                XSQLColumn col = null;
                for (XSQLBluePrintNode table : rs.getTables()) {
                    try {
                        col = table.findColumnByName(token);
                    } catch (Exception err) {
                    }
                    if (col != null) {
                        break;
                    }
                }
                if (col == null) {
                    throw new SQLException("Unknown field name '" + token
                            + "'.");
                }

                rs.getFields().add(col);
            }
        }
    }

    public static void parseCriteria(JDBCResultSet rs, XSQLBluePrint bp) {
        String lowSQL = rs.getSQL().toLowerCase();
        int where = lowSQL.indexOf("where");
        int order = lowSQL.indexOf("order");
        int whereTo = lowSQL.indexOf(";");

        if (where == -1) {
            return;
        }

        if (order != -1) {
            whereTo = order;
        }

        if(whereTo==-1) {
            whereTo=lowSQL.length();
        }

        String whereStatement = rs.getSQL().substring(where + 5, whereTo)
                .trim();
        XSQLCriteria cr = new XSQLCriteria(whereStatement, -1);
        for (XSQLBluePrintNode tbl : rs.getTables()) {
            for (XSQLColumn col : tbl.getColumns()) {
                String colCriteria = cr.getCriteriaForProperty(col);
                if (colCriteria != null && !colCriteria.trim().equals("")) {
                    addCriteria(col, new XSQLCriteria(colCriteria, -1), rs);
                }
            }
        }
    }
}
