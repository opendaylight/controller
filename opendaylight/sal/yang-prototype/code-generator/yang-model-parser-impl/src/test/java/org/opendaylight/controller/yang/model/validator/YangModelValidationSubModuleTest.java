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

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Belongs_to_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_stmtContext;
import org.opendaylight.controller.yang.model.parser.util.YangValidationException;

public class YangModelValidationSubModuleTest {

    private YangModelBasicValidationListener valid;

    @Before
    public void setUp() {
        valid = new YangModelBasicValidationListener();
    }

    @Test(expected = YangValidationException.class)
    public void testNoRevision() {

        Submodule_stmtContext ctx = YangModelValidationTest.mockStatement(
                Submodule_stmtContext.class, "submodule1");

        try {
            valid.enterSubmodule_stmt(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing Submodule-header statement in Submodule:submodule"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testNoBelongsTo() {
        Submodule_header_stmtsContext header = mock(Submodule_header_stmtsContext.class);
        mockSubmoduleParent(header, "submodule");

        try {
            valid.enterSubmodule_header_stmts(header);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing Belongs-to statement in Submodule-header:"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testBelongsToNoPrefix() {
        Belongs_to_stmtContext belongsTo = YangModelValidationTest
                .mockStatement(Belongs_to_stmtContext.class, "supermodule");

        mockSubmoduleParent(belongsTo, "submodule");

        try {
            valid.enterBelongs_to_stmt(belongsTo);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Missing Prefix statement in Belongs-to:supermodule"));
            throw e;
        }
    }

    private Submodule_stmtContext mockSubmoduleParent(ParseTree child,
            String moduleName) {
        Submodule_stmtContext ctx = YangModelValidationTest.mockStatement(
                Submodule_stmtContext.class, moduleName);
        YangModelValidationTest.addChild(ctx, child);
        return ctx;
    }
}
