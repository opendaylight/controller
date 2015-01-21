package org.opendaylight.controller.md.sal.dom.xsql.jdbc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrint;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrintNode;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLColumn;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCServer extends Thread implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(XSQLAdapter.class);

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

    public static void execute(JDBCResultSet rs, XSQLAdapter adapter)
        throws SQLException {
        parseTables(rs, adapter.getBluePrint());
        parseFields(rs, adapter.getBluePrint());
        parseCriteria(rs, adapter.getBluePrint());
        try {
            adapter.execute(rs);
        } catch (Exception err) {
            throw new SQLException("Error", err);
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
                throw new SQLException(
                    "Unknown table name \"" + tableName + "\"");
            }
            rs.addTableToQuery(table);
        }
    }

    public static void addCriteria(XSQLColumn col, XSQLCriteria c,
        JDBCResultSet rs) {
        Map<XSQLColumn, List<XSQLCriteria>> tblCriteria =
            rs.getCriteria().get(col.getTableName());
        if (tblCriteria == null) {
            tblCriteria =
                new ConcurrentHashMap<XSQLColumn, List<XSQLCriteria>>();
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
                XSQLBluePrintNode tbl = bp.getBluePrintNodeByTableName(
                    token.substring(0, token.indexOf(".")).trim());
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
                    throw new SQLException(
                        "Unknown field name '" + token + "'.");
                }

                rs.getFields().add(col);
            }
        }
    }

    public static void parseCriteria(JDBCResultSet rs, XSQLBluePrint bp) {
        String lowSQL = rs.getSQL().toLowerCase();
        int where = lowSQL.indexOf("where");
        int order = lowSQL.indexOf("order");
        int subQuery = lowSQL.indexOf("select", 2);
        int whereTo = lowSQL.indexOf(";");

        if (where == -1) {
            return;
        }

        if (where != -1 && subQuery != -1 && subQuery < where) {
            return;
        }

        if (order != -1 && subQuery != -1 && order < subQuery) {
            whereTo = order;
        } else if (order != -1 && subQuery != -1 && order > subQuery) {
            whereTo = subQuery;
        } else if (order != -1) {
            whereTo = order;
        } else if (subQuery != -1) {
            whereTo = subQuery;
        }
        String whereStatement =
            rs.getSQL().substring(where + 5, whereTo).trim();
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

    @Override
    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.warn("Exception while trying to close socket: {}", e);
            }
        }
    }
}
