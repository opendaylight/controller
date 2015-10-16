/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.odl.sql;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/*
 * BluePrint is the container of blue print nodes, a blue print nodes represents a schema context model node in the yang
 * model tree structure. odl-sql is registered on schema context changes and whenever it get a notification
 * it is introspect the schema module data stracture and creates Blue Print Nodes structure that
 * describes the schema context in a way that odl-sql can traverse the model up & down.
 */
public class BluePrint implements DatabaseMetaData, Serializable {
    //this file is serialized when a JDBC connection is connected so the
    //other side could get the schema description of the virtual database
    private static final long serialVersionUID = 1L;
    //For testing puposes, the odl-sql can export the blue print to a file to
    //be loaded by tests.
    public static final String CACHE_FILE_NAME = "./BluePrintCache.dat";

    //Short table name to blue print node map for query parsing
    private Map<String, BluePrintNode> tableNameToBluePrint = new HashMap<String, BluePrintNode>();
    //full odl table path name to node map
    private Map<String, Map<String, BluePrintNode>> odlNameToBluePrint = new HashMap<String, Map<String, BluePrintNode>>();

    private boolean cacheLoadedSuccessfuly = false;
    //A proxy object to print out method call
    //when debugging a third party tool using the odl sql JDBC
    private DatabaseMetaData myProxy = null;

    //A static method to replace a string with another inside a string
    //without regular expression escaping
    public static final String replaceAll(String source, String toReplace,String withThis) {
        int index = source.indexOf(toReplace);
        int index2 = 0;
        StringBuffer result = new StringBuffer();
        while (index != -1) {
            result.append(source.substring(index2, index));
            result.append(withThis);
            index2 = index + toReplace.length();
            index = source.indexOf(toReplace, index2);
        }
        if (index2 < source.length()) {
            result.append(source.substring(index2));
        }
        return result.toString();
    }

    public BluePrint() {
    }

    //Saves the blue print to a file
    public static void save(BluePrint bp) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new DataOutputStream(
                    new FileOutputStream(CACHE_FILE_NAME)));
            out.writeObject(bp);
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception err) {
            }
        }
    }

    //Loads the blue print from a file
    public static BluePrint load(InputStream ins) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new DataInputStream(ins));
            return (BluePrint) in.readObject();
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception err) {
            }
        }
        return null;
    }

    //The proxy file to printout the method name that was called
    //used for debugging a third party connection via JDBC
    private class BluePrintProxy implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            System.out.println("Method " + method);
            return method.invoke(BluePrint.this, args);
        }
    }

    //return an instance of a proxy
    public DatabaseMetaData getProxy() {
        if (myProxy == null) {
            try {
                myProxy = (DatabaseMetaData) Proxy.newProxyInstance(getClass()
                        .getClassLoader(),
                        new Class[] { DatabaseMetaData.class },
                        new BluePrintProxy());
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return myProxy;
    }

    //return a list of tables that their odl simple table name is identical but their table name is not
    public BluePrintNode[] getBluePrintNodeByODLTableName(String odlTableName) {
        Map<String, BluePrintNode> map = this.odlNameToBluePrint.get(odlTableName);
        if (map == null) {
            return null;
        }
        return map.values().toArray(new BluePrintNode[map.size()]);
    }

    //Finds the table according to a substring of its name
    public BluePrintNode getBluePrintNodeByTableName(String tableName) {
        if (tableName.indexOf(".") != -1) {
            tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
        }

        BluePrintNode node = tableNameToBluePrint.get(tableName);

        if (node != null) {
            return node;
        }

        for (BluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().endsWith(tableName)) {
                return n;
            }
        }

        for (BluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().toLowerCase()
                    .endsWith(tableName.toLowerCase())) {
                return n;
            }
        }

        for (BluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().toLowerCase()
                    .equals(tableName.toLowerCase())) {
                return n;
            }
        }

        for (BluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().toLowerCase()
                    .indexOf(tableName.toLowerCase()) != -1) {
                return n;
            }
        }
        return null;
    }

    public boolean isCacheLoaded() {
        return cacheLoadedSuccessfuly;
    }

    //Adds a node to the blueprint and connect it
    //to a parent node
    public BluePrintNode addToBluePrintCache(BluePrintNode blNode,BluePrintNode parent) {
        BluePrintNode existingNode = this.tableNameToBluePrint.get(blNode.getBluePrintNodeName());
        //if the node already exist
        if(existingNode!=null){
            //It might be that this is an augmentation of the node
            //or it is the same node but from a different model version
            //so merge the node to the existing one.
            existingNode.mergeAugmentation(blNode);
            //Need to make sure the table exist in the table name to node map
            String tableNames[] = blNode.getODLTableNames();
            for(String tableName:tableNames){
                Map<String, BluePrintNode> map = this.odlNameToBluePrint.get(tableName);
                if (map == null) {
                    map = new HashMap<String, BluePrintNode>();
                    this.odlNameToBluePrint.put(tableName, map);
                }
                map.put(blNode.getBluePrintNodeName(), blNode);
            }
            return existingNode;
        }else{
            //insert the node and update the table name to node map
            this.tableNameToBluePrint.put(blNode.getBluePrintNodeName(), blNode);
            String tableNames[] = blNode.getODLTableNames();
            for(String tableName:tableNames){
                Map<String, BluePrintNode> map = this.odlNameToBluePrint.get(tableName);
                if (map == null) {
                    map = new HashMap<String, BluePrintNode>();
                    this.odlNameToBluePrint.put(tableName, map);
                }
                map.put(blNode.getBluePrintNodeName(), blNode);
            }
            //Attach this node to its parent
            if(parent!=null)
                parent.addChild(blNode);
            return blNode;
        }
    }

    //Return a list of all table names
    public List<String> getAllTableNames() {
        List<String> names = new ArrayList<String>();
        for (BluePrintNode n : this.tableNameToBluePrint.values()) {
            if (!n.isModule() && !n.getColumns().isEmpty()) {
                names.add(n.getBluePrintNodeName());
            }
        }
        return names;

    }

    /*
     * From this point onwards this is the implementation of the JDBC Meta data.(non-Javadoc)
     *
     */

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        return false;
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        return true;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern,
            String typeNamePattern, String attributeNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema,
            String table, int scope, boolean nullable) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema,
            String table, String columnNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog,
            String parentSchema, String parentTable, String foreignCatalog,
            String foreignSchema, String foreignTable) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        return "OpenDayLight";
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return "0.1";
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDriverMajorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDriverMinorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getDriverName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDriverVersion() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern,
            String functionNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern,
            String functionNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table,
            boolean unique, boolean approximate) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxConnections() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxStatements() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern,
            String procedureNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern,
            String procedureNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSQLStateType() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getStringFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern,
            String tableNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern,
            String typeNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern,
            String tableNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern,
            String tableNamePattern, String[] types) throws SQLException {
        return new TablesResultSet(this);
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern,
            String typeNamePattern, int[] types) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getURL() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUserName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema,
            String table) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsConvert(int fromType, int toType)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly()
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        // TODO Auto-generated method stub
        return false;
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
    public ResultSet getPseudoColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

}
