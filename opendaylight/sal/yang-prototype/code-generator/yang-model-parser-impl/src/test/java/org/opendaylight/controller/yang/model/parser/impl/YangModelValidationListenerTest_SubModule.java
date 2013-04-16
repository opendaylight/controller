package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.mockito.Mockito.*;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Belongs_to_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_stmtContext;

public class YangModelValidationListenerTest_SubModule {

    private YangModelValidationListener valid;
    private Submodule_stmtContext ctx;

    @Before
    public void setUp() {
        valid = new YangModelValidationListener();
    }

    @Test(expected = YangValidationException.class)
    public void testNoRevision() {

        Submodule_stmtContext ctx = mock(Submodule_stmtContext.class);
        doReturn(1).when(ctx).getChildCount();
        YangModelValidationListenerTest_Module.mockName(ctx, "submodule1");

        try {
            valid.enterSubmodule_stmt(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing revision statements in submodule:submodule1"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoHeaderStmts() {
        mockSubmoduleWithRevision(2, "1999-4-5", "submodule");

        try {
            valid.enterSubmodule_stmt(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing header statements in submodule:submodule"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoBelongsTo() {
        Submodule_header_stmtsContext header = mock(Submodule_header_stmtsContext.class);
        mockSubmoduleParent(2, header, "submodule");

        try {
            valid.enterSubmodule_header_stmts(header);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing belongs-to statement in submodule:submodule"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testBelongsToNoPrefix() {
        Belongs_to_stmtContext belongsTo = mock(Belongs_to_stmtContext.class);
        doReturn(1).when(belongsTo).getChildCount();
        YangModelValidationListenerTest_Module.mockName(belongsTo,
                "supermodule");

        mockSubmoduleParent(2, belongsTo, "submodule");

        try {
            valid.enterBelongs_to_stmt(belongsTo);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing prefix statement in belongs-to:supermodule, in (sub)module:submodule"));
            throw e;
        }
    }

    @Test
    public void testBelongsTo() {
        Belongs_to_stmtContext belongsTo = mock(Belongs_to_stmtContext.class);
        doReturn(2).when(belongsTo).getChildCount();
        YangModelValidationListenerTest_Module.mockName(belongsTo,
                "supermodule");

        Prefix_stmtContext prefix = mock(Prefix_stmtContext.class);
        doReturn(prefix).when(belongsTo).getChild(1);
        doReturn(belongsTo).when(prefix).getParent();

        mockSubmoduleParent(2, belongsTo, "submodule");
        valid.enterBelongs_to_stmt(belongsTo);

    }

    private Revision_stmtContext mockSubmoduleWithRevision(int moduleChildren,
            String date, String nameOfSubmodule) {
        Revision_stmtContext mockedRev = mock(Revision_stmtContext.class);
        doReturn(1).when(mockedRev).getChildCount();
        YangModelValidationListenerTest_Module.mockName(mockedRev, date);

        Revision_stmtsContext revs = YangModelValidationListenerTest_Module
                .mockRevisionsParent(2, mockedRev);

        mockSubmoduleParent(moduleChildren, revs, nameOfSubmodule);
        return mockedRev;
    }

    private void mockSubmoduleParent(int moduleChildren, ParseTree child,
            String moduleName) {
        ctx = mock(Submodule_stmtContext.class);
        doReturn(moduleChildren).when(ctx).getChildCount();
        YangModelValidationListenerTest_Module.mockName(ctx, moduleName);
        doReturn(child).when(ctx).getChild(1);
        doReturn(ctx).when(child).getParent();
    }
}
