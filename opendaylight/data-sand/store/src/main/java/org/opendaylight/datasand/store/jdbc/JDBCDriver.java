package org.opendaylight.datasand.store.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class JDBCDriver implements Driver {

    public static JDBCDriver drv = new JDBCDriver();

    public JDBCDriver() {
        try {
            DriverManager.registerDriver(this);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean acceptsURL(String arg0) throws SQLException {
        return true;
    }

    @Override
    public Connection connect(String url, Properties arg1) throws SQLException {
        System.err.println("JDBC Connection");
        try {
            if (url.equals("svr")) {
                return new JDBCConnection(true);
            } else {
                return new JDBCConnection(url).getProxy();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        System.err.println("Error JDBC Connection");
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1)
        throws SQLException {
        DriverPropertyInfo i = new DriverPropertyInfo("OpenDayLight", "OpenDayLight");
        return new DriverPropertyInfo[] {i};
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

}
