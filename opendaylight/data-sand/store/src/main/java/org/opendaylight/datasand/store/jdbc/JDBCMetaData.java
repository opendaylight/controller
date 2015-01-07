package org.opendaylight.datasand.store.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;

public class JDBCMetaData implements DatabaseMetaData{
    private Map<String,TypeDescriptor> types = new ConcurrentHashMap<String, TypeDescriptor>();
    private Map<String,TypeDescriptor> shortNameToType = new ConcurrentHashMap<String, TypeDescriptor>();

    public JDBCMetaData(byte[] mdsalTableRepositoryData){
        ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(mdsalTableRepositoryData,null);
        int size = ba.getEncoder().decodeInt16(ba);
        for(int i=0;i<size;i++){
            TypeDescriptor type = TypeDescriptor.decode(ba);
            types.put(type.getTypeClassName(), type);
            int index = type.getTypeClassName().lastIndexOf(".");
            shortNameToType.put(type.getTypeClassName().substring(index+1), type);
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
    public boolean allProceduresAreCallable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
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
    public boolean deletesAreDetected(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public ResultSet getAttributes(String arg0, String arg1, String arg2,
            String arg3) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getBestRowIdentifier(String arg0, String arg1,
            String arg2, int arg3, boolean arg4) throws SQLException {
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
    public ResultSet getCatalogs() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getColumnPrivileges(String arg0, String arg1, String arg2,
            String arg3) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getColumns(String arg0, String arg1, String arg2,
            String arg3) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getCrossReference(String arg0, String arg1, String arg2,
            String arg3, String arg4, String arg5) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public String getDatabaseProductName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getDatabaseProductVersion() throws SQLException {
        // TODO Auto-generated method stub
        return null;
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
    public ResultSet getExportedKeys(String arg0, String arg1, String arg2)
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
    public ResultSet getFunctionColumns(String arg0, String arg1, String arg2,
            String arg3) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getFunctions(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getIdentifierQuoteString() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getImportedKeys(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getIndexInfo(String arg0, String arg1, String arg2,
            boolean arg3, boolean arg4) throws SQLException {
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
    public ResultSet getPrimaryKeys(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getProcedureColumns(String arg0, String arg1, String arg2,
            String arg3) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getProcedureTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getProcedures(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getPseudoColumns(String arg0, String arg1, String arg2,
            String arg3) throws SQLException {
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
    public String getSchemaTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getSchemas() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getSchemas(String arg0, String arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getSearchStringEscape() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getStringFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getSuperTables(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getSuperTypes(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getSystemFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getTablePrivileges(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getTableTypes() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ResultSet getTables(String arg0, String arg1, String arg2,
            String[] arg3) throws SQLException {
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
    public ResultSet getUDTs(String arg0, String arg1, String arg2, int[] arg3)
            throws SQLException {
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
    public ResultSet getVersionColumns(String arg0, String arg1, String arg2)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean insertsAreDetected(int arg0) throws SQLException {
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
    public boolean othersDeletesAreVisible(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean othersInsertsAreVisible(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean othersUpdatesAreVisible(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean ownDeletesAreVisible(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean ownInsertsAreVisible(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean ownUpdatesAreVisible(int arg0) throws SQLException {
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
    public boolean supportsConvert(int arg0, int arg1) throws SQLException {
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
    public boolean supportsResultSetConcurrency(int arg0, int arg1)
            throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean supportsResultSetHoldability(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean supportsResultSetType(int arg0) throws SQLException {
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
    public boolean supportsTransactionIsolationLevel(int arg0)
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
    public boolean updatesAreDetected(int arg0) throws SQLException {
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
}
