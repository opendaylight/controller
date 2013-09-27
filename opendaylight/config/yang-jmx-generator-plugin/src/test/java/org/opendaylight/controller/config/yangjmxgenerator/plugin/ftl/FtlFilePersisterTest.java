/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDeclaration;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FormattingUtil;

import com.google.common.collect.Lists;

public class FtlFilePersisterTest {
    private final FtlFilePersister tested = new FtlFilePersister();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGeneralInterface() {
        String packageName = "pa.cka.ge";
        String name = "GeneralClassImpl";
        List<String> extendedInterfaces = Arrays.asList("List", "Set");
        List<MethodDeclaration> methods = new ArrayList<>();
        methods.add(new MethodDeclaration("String", "executeOperation",
                Collections.<Field> emptyList()));

        List<String> mods = Lists.newArrayList();
        List<String> mods2 = Lists.newArrayList("final");
        methods.add(new MethodDeclaration("String", "executeOperation", Arrays
                .asList(new Field(mods, "int", "param1"), new Field(mods2, "long", "param2"))));

        GeneralInterfaceTemplate generalInterface = new GeneralInterfaceTemplate(
                null, packageName, name, extendedInterfaces, methods);

        Map<FtlTemplate, String> abstractFtlFileStringMap = tested
                .serializeFtls(Arrays.asList(generalInterface));
        String content = FormattingUtil
                .cleanUpEmptyLinesAndIndent(abstractFtlFileStringMap.get(generalInterface));

        // skip header
        content = content.substring(content.indexOf("package"));

        String expected = "package pa.cka.ge;\n"
                + "/**\n"
                + "*\n"
                + "*/\n"
                + "public interface GeneralClassImpl extends List, Set\n{\n"
                + "public String executeOperation();\n"
                + "public String executeOperation(int param1, final long param2);\n"
                + "}\n";

        assertEquals(expected, content);
    }

}
