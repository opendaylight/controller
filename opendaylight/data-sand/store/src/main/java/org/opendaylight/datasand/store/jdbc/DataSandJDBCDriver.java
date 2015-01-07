/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class DataSandJDBCDriver implements Driver {

    public static DataSandJDBCDriver drv = new DataSandJDBCDriver();
    static {
        ByteEncoder.registerSerializer(DataSandJDBCMessage.class, new DataSandJDBCMessage(), 432);
        ByteEncoder.registerSerializer(DataSandJDBCDataContainer.class, new DataSandJDBCDataContainer(), 433);
    }
    public DataSandJDBCDriver() {
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
        System.err.println("Data Sand JDBC Connection");
        try {
            TypeDescriptorsContainer tsc = new TypeDescriptorsContainer("JDBC-Connection");
            AutonomousAgentManager manager = new AutonomousAgentManager(tsc,true);
            return new DataSandJDBCConnection(manager, url);
        } catch (Exception err) {
            err.printStackTrace();
        }
        System.err.println("Error Data Sand JDBC Connection");
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
        DriverPropertyInfo i = new DriverPropertyInfo("DataSand", "DataSand");
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
