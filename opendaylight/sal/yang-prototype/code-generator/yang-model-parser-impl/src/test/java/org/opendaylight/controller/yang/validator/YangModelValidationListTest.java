/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.validator;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Default_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Key_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leaf_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.List_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Unique_stmtContext;
import org.opendaylight.controller.yang.parser.util.YangValidationException;

public class YangModelValidationListTest {

    private YangModelBasicValidationListener valid;

    @Before
    public void setUp() {
        valid = new YangModelBasicValidationListener();
    }

    @Test(expected = YangValidationException.class)
    public void testKeyValidationDuplicates() {

        List_stmtContext list = YangModelValidationTest.mockStatement(
                List_stmtContext.class, "list");
        Key_stmtContext key = YangModelValidationTest.mockStatement(
                Key_stmtContext.class, "leaf1 leaf2 leaf1 leaf1");
        YangModelValidationTest.addChild(list, key);

        try {
            valid.enterKey_stmt(key);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(),
                    containsString("contains duplicates:[leaf1]"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testUniqueValidationDuplicates() {
        List_stmtContext list = YangModelValidationTest.mockStatement(
                List_stmtContext.class, "list");
        Unique_stmtContext unique = YangModelValidationTest.mockStatement(
                Unique_stmtContext.class, "leaf1/a leaf2/n leaf1/a leaf1");
        YangModelValidationTest.addChild(list, unique);

        try {
            valid.enterUnique_stmt(unique);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(),
                    containsString("contains duplicates:[leaf1/a]"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testOrderBy() {
        Ordered_by_argContext ctx = YangModelValidationTest.mockStatement(
                Ordered_by_argContext.class, "unknown");

        try {
            valid.enterOrdered_by_arg(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Ordered-by:unknown, illegal value for Ordered-by statement, only permitted:"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testLeaf() {
        Leaf_stmtContext ctx = YangModelValidationTest.mockStatement(
                Leaf_stmtContext.class, "leaf1");
        Default_stmtContext def = YangModelValidationTest.mockStatement(
                Default_stmtContext.class, "default");
        YangModelValidationTest.addChild(ctx, def);
        Type_stmtContext typ = YangModelValidationTest.mockStatement(
                Type_stmtContext.class, "type");
        YangModelValidationTest.addChild(ctx, def);
        YangModelValidationTest.addChild(ctx, typ);

        Mandatory_stmtContext mand = YangModelValidationTest.mockStatement(
                Mandatory_stmtContext.class, null);
        YangModelValidationTest.addChild(ctx, mand);

        try {
            valid.enterLeaf_stmt(ctx);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Both Mandatory and Default statement present"));
            throw e;
        }
    }

}
