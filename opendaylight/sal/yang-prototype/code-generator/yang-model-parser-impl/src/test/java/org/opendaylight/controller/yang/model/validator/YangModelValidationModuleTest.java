/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.validator;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Namespace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yang_version_stmtContext;
import org.opendaylight.controller.yang.model.parser.util.YangValidationException;

public class YangModelValidationModuleTest {

    private YangModelBasicValidationListener valid;

    @Before
    public void setUp() {
        valid = new YangModelBasicValidationListener();
    }

    @Test(expected = YangValidationException.class)
    public void testRevisionInvalidDateFormat() {
        Revision_stmtContext mockedRev = mockModuleWithRevision("badFormat",
                "module1");

        try {
            valid.enterRevision_stmt(mockedRev);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Revision:badFormat, invalid date format expected date format is:"));
            throw e;
        }
    }

    @Test
    public void testRevisionValidDateFormat() {
        Revision_stmtContext mockedRev = mockModuleWithRevision(
                YangModelValidationTest.getFormattedDate(), "module1");

        valid.enterRevision_stmt(mockedRev);
    }

    @Test(expected = YangValidationException.class)
    public void testNoHeaderStmts() {
        Revision_stmtContext rev = mockModuleWithRevision("1999-4-5", "module1");

        try {
            valid.enterModule_stmt((Module_stmtContext) rev.getParent()
                    .getParent());
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing Module-header statement in Module:module1"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testMultipleModulesPerSession() {
        Module_stmtContext module1 = (Module_stmtContext) mockModuleWithRevision(
                "1999-09-10", "m1").getParent().getParent();
        YangModelValidationTest.addChild(module1, YangModelValidationTest
                .mockStatement(Namespace_stmtContext.class, ""));

        Module_stmtContext module2 = (Module_stmtContext) mockModuleWithRevision(
                "1999-09-10", "m2").getParent().getParent();
        YangModelValidationTest.addChild(module1, YangModelValidationTest
                .mockStatement(Namespace_stmtContext.class, ""));
        valid.enterModule_stmt(module1);

        try {
            valid.enterModule_stmt(module2);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(),
                    containsString("Multiple (sub)modules per file"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoNamespace() {
        Module_header_stmtsContext header = YangModelValidationTest
                .mockStatement(Module_header_stmtsContext.class, null);
        Module_stmtContext mod = YangModelValidationTest.mockStatement(
                Module_stmtContext.class, "module1");
        YangModelValidationTest.addChild(mod, header);

        try {
            valid.enterModule_header_stmts(header);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing Namespace statement in Module-header:"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoPrefix() {
        Module_header_stmtsContext header = YangModelValidationTest
                .mockStatement(Module_header_stmtsContext.class, null);
        Namespace_stmtContext nmspc = YangModelValidationTest.mockStatement(
                Namespace_stmtContext.class, "http://test");
        Module_stmtContext mod = YangModelValidationTest.mockStatement(
                Module_stmtContext.class, "module1");
        YangModelValidationTest.addChild(mod, header);
        YangModelValidationTest.addChild(header, nmspc);

        try {
            valid.enterModule_header_stmts(header);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing Prefix statement in Module-header:"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testInvalidYangVersion() {

        Yang_version_stmtContext yangVersion = YangModelValidationTest
                .mockStatement(Yang_version_stmtContext.class, "55Unsup");

        Module_stmtContext mod = YangModelValidationTest.mockStatement(
                Module_stmtContext.class, "module1");
        YangModelValidationTest.addChild(mod, yangVersion);

        try {
            valid.enterYang_version_stmt(yangVersion);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Unsupported yang version:55Unsup, supported version:"
                            + BasicValidations.SUPPORTED_YANG_VERSION));
            throw e;
        }
    }

    @Test
    public void testValidYangVersion() {

        Yang_version_stmtContext ctx = mock(Yang_version_stmtContext.class);
        doReturn(1).when(ctx).getChildCount();
        YangModelValidationTest.mockName(ctx, "1");

        valid.enterYang_version_stmt(ctx);
    }

    private static Revision_stmtContext mockModuleWithRevision(String date,
            String moduleName) {
        Revision_stmtContext mockedRev = YangModelValidationTest.mockStatement(
                Revision_stmtContext.class, date);
        Revision_stmtsContext revs = YangModelValidationTest.mockStatement(
                Revision_stmtsContext.class, null);
        Module_stmtContext mod = YangModelValidationTest.mockStatement(
                Module_stmtContext.class, moduleName);

        YangModelValidationTest.addChild(revs, mockedRev);
        YangModelValidationTest.addChild(mod, revs);
        return mockedRev;
    }
}
