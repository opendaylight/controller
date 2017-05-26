/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.rpc;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RuntimeRpcElementResolvedTest {

    private static final String MODULE_TYPE = "moduleType";
    private static final String INSTANCE_NAME = "instanceName";
    @Parameterized.Parameter(0)
    public String xpath;
    @Parameterized.Parameter(1)
    public Map<String, String> additional;

    @Parameterized.Parameters(name = "{index}: parsed({0}) contains moduleName:{1} and instanceName:{2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // With namespaces
                {"/a:modules/a:module[a:name='instanceName'][a:type='moduleType']/b:listener-state[b:peer-id='127.0.0.1']",
                    new HashMap<>(ImmutableMap.of("listener-state", "127.0.0.1"))},
                {"/a:modules/a:module[a:name='instanceName'][a:type='moduleType']",
                    null},

                // Without namespaces
                {"/modules/module[name=instanceName][type=moduleType]", null},
                {"/modules/module[type=moduleType][name='instanceName']", null},
                {"/modules/module[name=\'instanceName\'][type=\"moduleType\"]", null},
                {"/modules/module[type=moduleType and name=instanceName]", null},
                {"/modules/module[name=\"instanceName\" and type=moduleType]", null},
                {"/modules/module[type=\"moduleType\" and name=instanceName]", null},
                {"/modules/module[name=\'instanceName\' and type=\"moduleType\"]", null},

                // With inner beans
                {"/modules/module[name=instanceName and type=\"moduleType\"]/inner[key=b]", Collections.singletonMap("inner", "b")},
                {"/modules/module[name=instanceName and type=moduleType]/inner[key=b]", Collections.singletonMap("inner", "b")},
                {"/modules/module[name=instanceName and type=moduleType]/inner[key=\'b\']", Collections.singletonMap("inner", "b")},
                {"/modules/module[name=instanceName and type=moduleType]/inner[key=\"b\"]", Collections.singletonMap("inner", "b")},

                {"/modules/module[name=instanceName and type=\"moduleType\"]/inner[key2=a]/inner2[key=b]",
                    new HashMap<>(ImmutableMap.of("inner", "a", "inner2", "b"))
                },
        });
    }

    @Test
    public void testFromXpath() throws Exception {
        final RuntimeRpcElementResolved resolved = RuntimeRpcElementResolved.fromXpath(xpath, "element", "namespace");
        assertEquals(MODULE_TYPE, resolved.getModuleName());
        assertEquals(INSTANCE_NAME, resolved.getInstanceName());
        if (additional != null) {
            assertEquals(additional, resolved.getAdditionalAttributes());
        }
    }
}
