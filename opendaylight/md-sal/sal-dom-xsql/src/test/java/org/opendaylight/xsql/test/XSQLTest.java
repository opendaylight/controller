package org.opendaylight.xsql.test;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrint;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCResultSet;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCServer;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class XSQLTest {
    private static final String DATASTORE_TEST_YANG = "/sal-persisted-dom-test.yang";
    private XSQLBluePrint bluePrint = null;
    //private static SchemaContext schemaContext = null;
    @BeforeClass
    public static void loadSchemaContext(){
        //schemaContext = createTestContext();
    }

    @Before
    public void before() {
        try{
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("BluePrintCache.dat");
            if(in!=null){
                bluePrint = XSQLBluePrint.load(in);
                log("Loaded Blue Print!");
            }else{
                log("Can't find Blue Print!");
            }
            in.close();
        }catch(Exception err){
            err.printStackTrace();
        }
    }

    @Test
    public void testQueryParsingSimpleNoCriteria() {
        String sql = "select * from nodes/node;";
        JDBCResultSet rs = new JDBCResultSet(sql);
        parseTables(sql,bluePrint, rs);
        parseFields(sql, bluePrint, rs);
        JDBCServer.parseCriteria(rs, bluePrint);
        if(rs.getCriteria().isEmpty()){
            log("Test Criteria parsing of \""+sql+"\" Passed!");
            Assert.assertEquals(true, true);
        }else{
            log("Test Criteria parsing of \""+sql+"\" Failed!");
            Assert.assertEquals(false, true);
        }
    }

    @Test
    public void testQueryParsingComplexNoCriteria() {
        String sql = "select nodes/node.id,nodes/node/node-connector.id,nodes/node/node-connector.hardware-address from nodes/node,nodes/node/node-connector;";
        JDBCResultSet rs = new JDBCResultSet(sql);
        parseTables(sql,bluePrint, rs);
        parseFields(sql, bluePrint, rs);
        JDBCServer.parseCriteria(rs, bluePrint);
        if(rs.getCriteria().isEmpty()){
            log("Test Criteria parsing of \""+sql+"\" Passed!");
            Assert.assertEquals(true, true);
        }else{
            log("Test Criteria parsing of \""+sql+"\" Failed!");
            Assert.assertEquals(false, true);
        }
    }

    @Test
    public void testQueryParsingComplexWithCriteria() {
        String sql = "select nodes/node.id,nodes/node/node-connector.id,nodes/node/node-connector.hardware-address from nodes/node,nodes/node/node-connector where hardware-address like 'AB';";
        JDBCResultSet rs = new JDBCResultSet(sql);
        parseTables(sql,bluePrint, rs);
        parseFields(sql, bluePrint, rs);
        JDBCServer.parseCriteria(rs, bluePrint);
        if(!rs.getCriteria().isEmpty()){
            log("Test Criteria parsing of \""+sql+"\" Passed!");
            Assert.assertEquals(true, true);
        }else{
            log("Test Criteria parsing of \""+sql+"\" Failed!");
            Assert.assertEquals(false, true);
        }
    }

    @Test
    public void testQueryParsingSimpleWithCriteria() {
        String sql = "select * from nodes/node where nodes/node.id like 'something...';";
        JDBCResultSet rs = new JDBCResultSet(sql);
        parseTables(sql,bluePrint, rs);
        parseFields(sql, bluePrint, rs);
        JDBCServer.parseCriteria(rs, bluePrint);
        if(!rs.getCriteria().isEmpty()){
            log("Test Criteria parsing of \""+sql+"\" Passed!");
            Assert.assertEquals(true, true);
        }else{
            log("Test Criteria parsing of \""+sql+"\" Failed!");
            Assert.assertEquals(false, true);
        }
    }

    private static void parseTables(String sql,XSQLBluePrint bp,JDBCResultSet rs){
        try{
            JDBCServer.parseTables(rs, bp);
            log("Test Table parsing of \""+sql+"\" Passed!");
            Assert.assertEquals(true,true);
        }catch(SQLException err){
            log("Test Table parsing of \""+sql+"\" Failed!");
            err.printStackTrace();
            Assert.assertEquals(false,true);
        }
    }

    @Test
    public void testQueryParsingComplexWithCriteriaAndGrouping() {

        String sub_sql = "select nodes/node.id,nodes/node/node-connector.id,nodes/node/node-connector.hardware-address from nodes/node,nodes/node/node-connector where hardware-address like 'AB';";

        String sql = "SELECT DISTINCT"
                + "\"LOGICAL_TABLE_1\".\"nodes/node.id\" AS \"COL0\"\n"
                + ",\"LOGICAL_TABLE_1\".\"nodes/node.address\" AS \"COL1\"\n"
                + ",\"LOGICAL_TABLE_1\".\"nodes/node/node-connector.hardware-address\" AS \"COL2\"\n"
                + "FROM\n"
                + "("+sub_sql+") \"LOGICAL_TABLE_1\"\n";



        JDBCResultSet rs = new JDBCResultSet(sql);
        XSQLAdapter.getInstance().loadBluePrint();
        try{
            JDBCServer.checkAndBreakSubQueries(rs, XSQLAdapter.getInstance());
            if(rs.getSubQueries().isEmpty()){
                log("Logical table parsing for "+sql+" Failed!");
            }else{
                JDBCServer.parseExternalQuery(rs);
                log("Fields="+rs.getFields().size());
                Assert.assertEquals(rs.getFields().size(), 3);
                Assert.assertEquals(rs.getTables().size(), 1);
                Assert.assertEquals(rs.getTables().get(0).getODLTableName(), "LOGICAL_TABLE_1");

                JDBCResultSet subRS = rs.getSubQueries().values().iterator().next();
                parseTables(sql,bluePrint, subRS);
                parseFields(sql, bluePrint, subRS);
                JDBCServer.parseCriteria(subRS, bluePrint);
                if(!subRS.getCriteria().isEmpty()){
                    log("Test Criteria parsing of \""+sql+"\" Passed!");
                    Assert.assertEquals(true, true);
                }else{
                    log("Test Criteria parsing of \""+sql+"\" Failed!");
                    Assert.assertEquals(false, true);
                }
            }
        }catch(SQLException err){
            err.printStackTrace();
        }
    }

    private static void parseFields(String sql,XSQLBluePrint bp,JDBCResultSet rs){
        try{
            JDBCServer.parseFields(rs, bp);
            log("Test Fields parsing of \""+sql+"\" Passed!");
            Assert.assertEquals(true,true);
        }catch(SQLException err){
            log("Test Fields parsing of \""+sql+"\" Failed!");
            err.printStackTrace();
            Assert.assertEquals(false,true);
        }
    }

    private static void log(String str) {
        System.out.print("*** XSQL Tests -");
        System.out.println(str);
    }

    public static final InputStream getDatastoreTestInputStream() {
        return getInputStream(DATASTORE_TEST_YANG);
    }

    private static InputStream getInputStream(final String resourceName) {
        return XSQLTest.class.getResourceAsStream(DATASTORE_TEST_YANG);
    }

    public static SchemaContext createTestContext() {
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(Collections.singletonList(getDatastoreTestInputStream()));
        return parser.resolveSchemaContext(modules);
    }
}
