/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.validator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Augment_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_add_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_delete_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviation_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Import_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Include_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Namespace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_date_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.StringContext;
import org.opendaylight.controller.yang.model.parser.impl.YangParserListenerImpl;
import org.opendaylight.controller.yang.model.parser.util.YangValidationException;

import com.google.common.collect.Sets;

public class YangModelValidationTest {

    private YangModelBasicValidationListener valid;

    @Before
    public void setUp() {

        valid = new YangModelBasicValidationListener();
    }

    @Test
    public void testPrefixes() {
        Prefix_stmtContext pref = mockStatement(Prefix_stmtContext.class,
                "unique1");
        Module_stmtContext module = mockStatement(Module_stmtContext.class,
                "module1");
        addChild(module, pref);

        valid.enterPrefix_stmt(pref);

        pref = mockStatement(Prefix_stmtContext.class, "unique1");
        module = mockStatement(Module_stmtContext.class, "module1");
        addChild(module, pref);

        try {
            valid.enterPrefix_stmt(pref);
        } catch (Exception e) {
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testNamespace() {

        Namespace_stmtContext namespace = mockStatement(
                Namespace_stmtContext.class, "http://test.parsing.uri.com");
        Module_stmtContext module = mockStatement(Module_stmtContext.class,
                "module1");
        addChild(module, namespace);

        valid.enterNamespace_stmt(namespace);

        namespace = mockStatement(Namespace_stmtContext.class, "invalid uri");
        module = mockStatement(Module_stmtContext.class, "module1");
        addChild(module, namespace);

        try {
            valid.enterNamespace_stmt(namespace);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Namespace:invalid uri cannot be parsed as URI"));
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testImports() {
        Import_stmtContext impor = mockImport("unique1", "p1");
        Module_stmtContext mod = mockStatement(Module_stmtContext.class,
                "module1");
        addChild(mod, impor);

        valid.enterImport_stmt(impor);

        impor = mockImport("unique1", "p2");
        mod = mockStatement(Module_stmtContext.class, "module1");
        addChild(mod, impor);

        try {
            valid.enterImport_stmt(impor);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(),
                    containsString("Import:unique1 not unique"));
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testIncludes() {
        Include_stmtContext incl = mockInclude("unique1");
        Module_stmtContext mod = mockStatement(Module_stmtContext.class,
                "module1");
        addChild(mod, incl);
        valid.enterInclude_stmt(incl);

        incl = mockInclude("unique1");
        mod = mockStatement(Module_stmtContext.class, "module1");
        addChild(mod, incl);

        try {
            valid.enterInclude_stmt(incl);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(),
                    containsString("Include:unique1 not unique in (sub)module"));
            return;
        }

        fail("Validation Exception should have occured");
    }

    @Test
    public void testIdentifierMatching() {
        List<String> ids = new ArrayList<String>();
        // valid
        ids.add("_ok98-.87.-.8...88-asdAD");
        ids.add("AA.bcd");
        ids.add("a");
        // invalid
        ids.add("9aa");
        ids.add("-");
        ids.add(".");

        int thrown = 0;
        for (String id : ids) {
            try {
                Module_stmtContext module = mock(Module_stmtContext.class);
                Token token = mock(Token.class);
                when(module.getStart()).thenReturn(token);
                BasicValidations.checkIdentifierInternal(
                        module, id);
            } catch (YangValidationException e) {
                thrown++;
            }
        }

        assertThat(thrown, is(3));
    }

    @Test(expected = YangValidationException.class)
    public void testAugument() {
        Augment_stmtContext augument = mockStatement(Augment_stmtContext.class,
                "/a:*abc/a:augument1");
        Module_stmtContext mod1 = mockStatement(Module_stmtContext.class,
                "mod1");
        addChild(mod1, augument);

        Token token = mock(Token.class);
        when(augument.getStart()).thenReturn(token);

        try {
            valid.enterAugment_stmt(augument);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("Schema node id:/a:*abc/a:augument1 not in required format, details:Prefixed id:a:*abc not in required format"));
            throw e;
        }
    }

    @Test
    public void testDeviate() {
        Deviation_stmtContext ctx = mockStatement(Deviation_stmtContext.class,
                "deviations");
        Deviate_add_stmtContext add = mockStatement(
                Deviate_add_stmtContext.class, "add");
        Deviate_delete_stmtContext del = mockStatement(
                Deviate_delete_stmtContext.class, "delete");

        addChild(ctx, add);
        addChild(ctx, del);

        valid.enterDeviation_stmt(ctx);

        HashSet<Class<? extends ParseTree>> types = Sets.newHashSet();
        types.add(Deviate_add_stmtContext.class);
        types.add(Deviate_delete_stmtContext.class);

        int count = ValidationUtil.countPresentChildrenOfType(ctx, types);
        assertThat(count, is(2));
    }

    @Test(expected = YangValidationException.class)
    public void testStatus() throws Exception {
        Status_argContext status = mockStatement(Status_argContext.class,
                "unknown");
        try {
            valid.enterStatus_arg(status);
        } catch (YangValidationException e) {
            assertThat(
                    e.getMessage(),
                    containsString("illegal value for Status statement, only permitted:"));
            throw e;
        }
    }

    private Import_stmtContext mockImport(String name, String prefixName) {
        Import_stmtContext impor = mockStatement(Import_stmtContext.class, name);

        Prefix_stmtContext prefix = mockStatement(Prefix_stmtContext.class,
                prefixName);
        Revision_date_stmtContext revDate = mockStatement(
                Revision_date_stmtContext.class, getFormattedDate());

        addChild(impor, prefix);
        addChild(impor, revDate);
        return impor;
    }

    static String getFormattedDate() {
        return YangParserListenerImpl.simpleDateFormat.format(new Date());
    }

    private Include_stmtContext mockInclude(String name) {
        Include_stmtContext incl = mockStatement(Include_stmtContext.class,
                name);

        Revision_date_stmtContext revDate = mockStatement(
                Revision_date_stmtContext.class, getFormattedDate());

        addChild(incl, revDate);
        return incl;
    }

    static void mockName(ParseTree stmt, String name) {
        StringContext nameCtx = mock(StringContext.class);
        ParseTree internalName = mock(ParseTree.class);
        doReturn(1).when(stmt).getChildCount();
        doReturn(name).when(internalName).getText();
        doReturn(internalName).when(nameCtx).getChild(0);
        doReturn(nameCtx).when(stmt).getChild(0);
    }

    static <T extends ParseTree> T mockStatement(Class<T> stmtType, String name) {
        T stmt = stmtType.cast(mock(stmtType));

        doReturn(0).when(stmt).getChildCount();

        if (name != null)
            mockName(stmt, name);
        return stmt;
    }

    static void addChild(ParseTree parent, ParseTree child) {
        int childCount = parent.getChildCount() + 1;
        doReturn(childCount).when(parent).getChildCount();
        doReturn(child).when(parent).getChild(childCount - 1);
        doReturn(parent).when(child).getParent();
    }

}
