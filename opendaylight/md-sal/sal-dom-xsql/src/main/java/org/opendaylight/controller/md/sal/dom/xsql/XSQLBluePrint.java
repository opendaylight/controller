/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/**
 * To be removed in Nitrogen
 */
@Deprecated
public class XSQLBluePrint implements DatabaseMetaData, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CACHE_FILE_NAME = "./BluePrintCache.dat";

    private final Map<String, XSQLBluePrintNode> tableNameToBluePrint = new HashMap<>();
    private final Map<String, Map<String, XSQLBluePrintNode>> odlNameToBluePrint = new HashMap<>();

    private final boolean cacheLoadedSuccessfuly = false;
    private DatabaseMetaData myProxy = null;

    public static final String replaceAll(final String source, final String toReplace,
            final String withThis) {
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

    XSQLBluePrint() {

    }

    public void save() {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new DataOutputStream(
                    new FileOutputStream(CACHE_FILE_NAME)));
            out.writeObject(this);
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception err) {
            }
        }
    }

    static XSQLBluePrint create(final SchemaContext context) {
        final XSQLBluePrint ret = new XSQLBluePrint();
        for (Module m : context.getModules()) {
            XSQLODLUtils.createOpenDaylightCache(ret, m);
        }

        return ret;
    }

    public static XSQLBluePrint load(final InputStream ins) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new DataInputStream(ins));
            return (XSQLBluePrint) in.readObject();
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

    private class NQLBluePrintProxy implements InvocationHandler {
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args)
                throws Throwable {
            System.out.println("Method " + method);
            return method.invoke(XSQLBluePrint.this, args);
        }
    }

    public DatabaseMetaData getProxy() {
        if (myProxy == null) {
            try {
                myProxy = (DatabaseMetaData) Proxy.newProxyInstance(getClass()
                        .getClassLoader(),
                        new Class[] { DatabaseMetaData.class },
                        new NQLBluePrintProxy());
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return myProxy;
    }

    public XSQLBluePrintNode[] getBluePrintNodeByODLTableName(
            final String odlTableName) {
        Map<String, XSQLBluePrintNode> map = this.odlNameToBluePrint
                .get(odlTableName);
        if (map == null) {
            return null;
        }
        return map.values().toArray(new XSQLBluePrintNode[map.size()]);
    }

    public XSQLBluePrintNode getBluePrintNodeByTableName(String tableName) {
        if (tableName.indexOf(".") != -1) {
            tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
        }

        XSQLBluePrintNode node = tableNameToBluePrint.get(tableName);

        if (node != null) {
            return node;
        }

        for (XSQLBluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().endsWith(tableName)) {
                return n;
            }
        }

        for (XSQLBluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().toLowerCase()
                    .endsWith(tableName.toLowerCase())) {
                return n;
            }
        }

        for (XSQLBluePrintNode n : tableNameToBluePrint.values()) {
            if (n.getBluePrintNodeName().toLowerCase()
                    .equals(tableName.toLowerCase())) {
                return n;
            }
        }

        for (XSQLBluePrintNode n : tableNameToBluePrint.values()) {
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

    private static Map<Class<?>, Set<Class<?>>> superClassMap = new HashMap<>();

    public static Set<Class<?>> getInheritance(final Class<?> myObjectClass,
            final Class<?> returnType) {

        if (returnType != null && myObjectClass.equals(returnType)) {
            return new HashSet<>();
        }
        Set<Class<?>> result = superClassMap.get(myObjectClass);
        if (result != null) {
            return result;
        }
        result = new HashSet<>();
        superClassMap.put(myObjectClass, result);
        if (returnType != null) {
            if (!returnType.equals(myObjectClass)) {
                Class<?> mySuperClass = myObjectClass.getSuperclass();
                while (mySuperClass != null) {
                    result.add(mySuperClass);
                    mySuperClass = mySuperClass.getSuperclass();
                }
                result.addAll(collectInterfaces(myObjectClass));
            }
        }
        return result;
    }

    public static Set<Class<?>> collectInterfaces(final Class<?> cls) {
        Set<Class<?>> result = new HashSet<>();
        Class<?> myInterfaces[] = cls.getInterfaces();
        if (myInterfaces != null) {
            for (Class<?> in : myInterfaces) {
                result.add(in);
                result.addAll(collectInterfaces(in));
            }
        }
        return result;
    }

    public XSQLBluePrintNode addToBluePrintCache(final XSQLBluePrintNode blNode,final XSQLBluePrintNode parent) {
        XSQLBluePrintNode existingNode = this.tableNameToBluePrint.get(blNode.getBluePrintNodeName());
        if(existingNode!=null){
            existingNode.mergeAugmentation(blNode);
            return existingNode;
        }else{
            this.tableNameToBluePrint.put(blNode.getBluePrintNodeName(), blNode);
            Map<String, XSQLBluePrintNode> map = this.odlNameToBluePrint.get(blNode.getODLTableName());
            if (map == null) {
                map = new HashMap<>();
                this.odlNameToBluePrint.put(blNode.getODLTableName(), map);
            }
            map.put(blNode.getBluePrintNodeName(), blNode);
            if(parent!=null) {
                parent.addChild(blNode);
            }
            return blNode;
        }
    }

    public Class<?> getGenericType(final ParameterizedType type) {
        Type[] typeArguments = type.getActualTypeArguments();
        for (Type typeArgument : typeArguments) {
            if (typeArgument instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) typeArgument;
                return (Class<?>) pType.getRawType();
            } else if (typeArgument instanceof Class) {
                return (Class<?>) typeArgument;
            }
        }
        return null;
    }

    public Class<?> getMethodReturnTypeFromGeneric(final Method m) {
        Type rType = m.getGenericReturnType();
        if (rType instanceof ParameterizedType) {
            return getGenericType((ParameterizedType) rType);
        }
        return null;
    }

    public List<String> getAllTableNames() {
        List<String> names = new ArrayList<>();
        for (XSQLBluePrintNode n : this.tableNameToBluePrint.values()) {
            if (!n.isModule() && !n.getColumns().isEmpty()) {
                names.add(n.getBluePrintNodeName());
            }
        }
        return names;

    }

    public List<String> getInterfaceNames(final XSQLBluePrintNode node) {
        Set<XSQLBluePrintNode> children = node.getChildren();
        List<String> names = new ArrayList<>();
        for (XSQLBluePrintNode n : children) {
            if (!n.isModule() && !n.getColumns().isEmpty()) {
                names.add(n.toString());
            }
            names.addAll(getInterfaceNames(n));
        }
        return names;
    }

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
    public boolean deletesAreDetected(final int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResultSet getAttributes(final String catalog, final String schemaPattern,
            final String typeNamePattern, final String attributeNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getBestRowIdentifier(final String catalog, final String schema,
            final String table, final int scope, final boolean nullable) throws SQLException {
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
    public ResultSet getColumnPrivileges(final String catalog, final String schema,
            final String table, final String columnNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getColumns(final String catalog, final String schemaPattern,
            final String tableNamePattern, final String columnNamePattern)
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
    public ResultSet getCrossReference(final String parentCatalog,
            final String parentSchema, final String parentTable, final String foreignCatalog,
            final String foreignSchema, final String foreignTable) throws SQLException {
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
    public ResultSet getExportedKeys(final String catalog, final String schema, final String table)
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
    public ResultSet getFunctionColumns(final String catalog, final String schemaPattern,
            final String functionNamePattern, final String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getFunctions(final String catalog, final String schemaPattern,
            final String functionNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getImportedKeys(final String catalog, final String schema, final String table)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getIndexInfo(final String catalog, final String schema, final String table,
            final boolean unique, final boolean approximate) throws SQLException {
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
    public ResultSet getPrimaryKeys(final String catalog, final String schema, final String table)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getProcedureColumns(final String catalog, final String schemaPattern,
            final String procedureNamePattern, final String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getProcedures(final String catalog, final String schemaPattern,
            final String procedureNamePattern) throws SQLException {
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
    public ResultSet getSchemas(final String catalog, final String schemaPattern)
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
    public ResultSet getSuperTables(final String catalog, final String schemaPattern,
            final String tableNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getSuperTypes(final String catalog, final String schemaPattern,
            final String typeNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getTablePrivileges(final String catalog, final String schemaPattern,
            final String tableNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getTables(final String catalog, final String schemaPattern,
            final String tableNamePattern, final String[] types) throws SQLException {
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
    public ResultSet getUDTs(final String catalog, final String schemaPattern,
            final String typeNamePattern, final int[] types) throws SQLException {
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
    public ResultSet getVersionColumns(final String catalog, final String schema,
            final String table) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean insertsAreDetected(final int type) throws SQLException {
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
    public boolean othersDeletesAreVisible(final int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(final int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(final int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(final int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(final int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(final int type) throws SQLException {
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
    public boolean supportsConvert(final int fromType, final int toType)
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
    public boolean supportsResultSetConcurrency(final int type, final int concurrency)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsResultSetHoldability(final int holdability)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsResultSetType(final int type) throws SQLException {
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
    public boolean supportsTransactionIsolationLevel(final int level)
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
    public boolean updatesAreDetected(final int type) throws SQLException {
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
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getPseudoColumns(final String catalog, final String schemaPattern,
            final String tableNamePattern, final String columnNamePattern)
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
