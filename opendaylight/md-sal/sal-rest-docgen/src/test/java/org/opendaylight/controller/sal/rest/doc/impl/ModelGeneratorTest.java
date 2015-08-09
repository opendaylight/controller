/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.doc.impl;

import com.google.common.base.Preconditions;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.File;
import java.util.HashSet;
import java.util.Map;


public class ModelGeneratorTest {

    private DocGenTestHelper helper;
    private SchemaContext schemaContext;

    @Before
    public void setUp() throws Exception {
        helper = new DocGenTestHelper();
        helper.setUp();
        schemaContext = new YangParserImpl().resolveSchemaContext(new HashSet<Module>(helper.getModules().values()));
    }

    @Test
    public void testConvertToJsonSchema() throws Exception {

        Preconditions.checkArgument(helper.getModules() != null, "No modules found");

        ModelGenerator generator = new ModelGenerator();

        for (Map.Entry<File, Module> m : helper.getModules().entrySet()) {
            if (m.getKey().getAbsolutePath().endsWith("opflex.yang")) {

                JSONObject jsonObject = generator.convertToJsonSchema(m.getValue(), schemaContext);
                Assert.assertNotNull(jsonObject);
            }
        }

    }
}
