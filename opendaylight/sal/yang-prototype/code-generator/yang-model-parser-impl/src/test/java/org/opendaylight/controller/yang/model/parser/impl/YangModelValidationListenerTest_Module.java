package org.opendaylight.controller.yang.model.parser.impl;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Import_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Include_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Namespace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_date_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.StringContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yang_version_stmtContext;

public class YangModelValidationListenerTest_Module {

    private YangModelValidationListener valid;
    private Module_stmtContext ctx;

    @Before
    public void setUp() {
        valid = new YangModelValidationListener();
    }

    @Test(expected = YangValidationException.class)
    public void testRevisionInvalidDateFormat() {
        Revision_stmtContext mockedRev = mockModuleWithRevision(2, "badFormat");

        try {
            valid.enterRevision_stmt(mockedRev);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Invalid date format for revision:badFormat in (sub)module:module1, expected date format is:"));
            throw e;
        }
    }

    private Revision_stmtContext mockModuleWithRevision(int moduleChildren,
            String date) {
        Revision_stmtContext mockedRev = mock(Revision_stmtContext.class);
        doReturn(1).when(mockedRev).getChildCount();
        mockName(mockedRev, date);

        Revision_stmtsContext revs = mockRevisionsParent(2, mockedRev);

        mockModuleParent(moduleChildren, revs, "module1");
        return mockedRev;
    }

    @Test
    public void testRevisionValidDateFormat() {
        Revision_stmtContext mockedRev = mockModuleWithRevision(2,
                getFormattedDate());

        valid.enterRevision_stmt(mockedRev);
    }

    private String getFormattedDate() {
        return YangModelParserListenerImpl.simpleDateFormat.format(new Date());
    }

    @Test(expected = YangValidationException.class)
    public void testNoRevision() {

        Module_stmtContext ctx = mock(Module_stmtContext.class);
        doReturn(1).when(ctx).getChildCount();
        mockName(ctx, "module1");

        try {
            valid.enterModule_stmt(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing revision statements in module:module1"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoHeaderStmts() {
        mockModuleWithRevision(2, "1999-4-5");

        try {
            valid.enterModule_stmt(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing header statements in module:module1"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoNamespace() {
        Module_header_stmtsContext header = mock(Module_header_stmtsContext.class);
        mockModuleParent(2, header, "module1");

        try {
            valid.enterModule_header_stmts(header);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing namespace statement in module:module1"));
            throw e;
        }
    }

    @Test
    public void testPrefixes() {
        Prefix_stmtContext pref = mock(Prefix_stmtContext.class);
        doReturn(1).when(pref).getChildCount();
        mockName(pref, "unique1");
        mockModuleParent(2, pref, "module1");
        valid.enterPrefix_stmt(pref);

        pref = mock(Prefix_stmtContext.class);
        doReturn(1).when(pref).getChildCount();
        mockName(pref, "unique1");
        mockModuleParent(2, pref, "module1");

        try {
            valid.enterPrefix_stmt(pref);
        } catch (Exception e) {
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testNamespace() {
        Namespace_stmtContext namespace = mock(Namespace_stmtContext.class);
        doReturn(1).when(namespace).getChildCount();
        mockName(namespace, "http://test.parsing.uri.com");
        mockModuleParent(2, namespace, "module1");
        valid.enterNamespace_stmt(namespace);

        namespace = mock(Namespace_stmtContext.class);
        doReturn(1).when(namespace).getChildCount();
        mockName(namespace, "invalid uri");
        mockModuleParent(2, namespace, "module1");
        try {
            valid.enterNamespace_stmt(namespace);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Namespace:invalid uri in module:module1 cannot be parsed as URI"));
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testImports() {
        Import_stmtContext impor = mockImport("unique1", "p1");
        mockModuleParent(2, impor, "module1");
        valid.enterImport_stmt(impor);

        impor = mockImport("unique1", "p2");
        mockModuleParent(2, impor, "module1");
        mockName(impor, "unique1");

        try {
            valid.enterImport_stmt(impor);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Module:unique1 imported twice in (sub)module:module1"));
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testIncludes() {
        Include_stmtContext impor = mockInclude("unique1");
        mockModuleParent(2, impor, "module1");
        valid.enterInclude_stmt(impor);

        impor = mockInclude("unique1");
        mockModuleParent(2, impor, "module1");
        mockName(impor, "unique1");

        try {
            valid.enterInclude_stmt(impor);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Submodule:unique1 included twice in (sub)module:module1"));
            return;
        }

        fail("Validation Exception should have occured");
    }

    private Import_stmtContext mockImport(String name, String prefixName) {
        Import_stmtContext impor = mock(Import_stmtContext.class);
        doReturn(3).when(impor).getChildCount();
        Prefix_stmtContext prefix = mock(Prefix_stmtContext.class);
        mockName(prefix, prefixName);
        doReturn(prefix).when(impor).getChild(1);
        Revision_date_stmtContext revDate = mock(Revision_date_stmtContext.class);
        mockName(revDate, getFormattedDate());
        doReturn(revDate).when(impor).getChild(2);
        mockName(impor, name);
        return impor;
    }

    private Include_stmtContext mockInclude(String name) {
        Include_stmtContext impor = mock(Include_stmtContext.class);
        doReturn(2).when(impor).getChildCount();
        Revision_date_stmtContext revDate = mock(Revision_date_stmtContext.class);
        mockName(revDate, getFormattedDate());
        doReturn(revDate).when(impor).getChild(1);
        mockName(impor, name);
        return impor;
    }

    @Test(expected = YangValidationException.class)
    public void testInvalidYangVersion() {

        Yang_version_stmtContext yangVersion = mock(Yang_version_stmtContext.class);
        doReturn(1).when(yangVersion).getChildCount();
        mockName(yangVersion, "55Unsup");

        mockModuleParent(2, yangVersion, "module1");

        try {
            valid.enterYang_version_stmt(yangVersion);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Unsupported yang version:55Unsup, in (sub)module:module1, supported version:"
                            + YangModelValidationListener.SUPPORTED_YANG_VERSION));
            throw e;
        }
    }

    private void mockModuleParent(int moduleChildren, ParseTree child,
            String moduleName) {
        ctx = mock(Module_stmtContext.class);
        doReturn(moduleChildren).when(ctx).getChildCount();
        mockName(ctx, moduleName);
        doReturn(child).when(ctx).getChild(1);
        doReturn(ctx).when(child).getParent();
    }

    static Revision_stmtsContext mockRevisionsParent(int moduleChildren,
            Revision_stmtContext mockedRev) {
        Revision_stmtsContext revs = mock(Revision_stmtsContext.class);
        doReturn(moduleChildren).when(revs).getChildCount();
        doReturn(mockedRev).when(revs).getChild(1);
        doReturn(revs).when(mockedRev).getParent();
        return revs;
    }

    @Test
    public void testValidYangVersion() {

        Yang_version_stmtContext ctx = mock(Yang_version_stmtContext.class);
        doReturn(1).when(ctx).getChildCount();
        mockName(ctx, "1");

        valid.enterYang_version_stmt(ctx);
    }

    @Test
    public void testIdentifierMatching() {
        YangModelValidationListener.checkIdentifier("_ok98-.87.-.8...88-asdAD",
                null);
        YangModelValidationListener.checkIdentifier("AA.bcd", null);
        YangModelValidationListener.checkIdentifier("a", null);

        int thrown = 0;

        try {
            YangModelValidationListener.checkIdentifier("9aa", null);
        } catch (YangValidationException e) {
            thrown++;
        }
        try {
            YangModelValidationListener.checkIdentifier("-", null);
        } catch (YangValidationException e) {
            thrown++;
        }
        try {
            YangModelValidationListener.checkIdentifier(".", null);
        } catch (YangValidationException e) {
            thrown++;
        }

        assertThat(thrown, is(3));
    }

    static void mockName(ParseTree mockedRev, String name) {
        StringContext nameCtx = mock(StringContext.class);
        ParseTree internalName = mock(ParseTree.class);
        doReturn(name).when(internalName).getText();
        doReturn(internalName).when(nameCtx).getChild(0);
        doReturn(nameCtx).when(mockedRev).getChild(0);
    }
}
