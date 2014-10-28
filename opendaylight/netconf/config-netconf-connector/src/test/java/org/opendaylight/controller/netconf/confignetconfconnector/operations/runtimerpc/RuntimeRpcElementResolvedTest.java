/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc;

import static org.junit.Assert.assertEquals;

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

    @Parameterized.Parameter(0)
    public String xpath;
    @Parameterized.Parameter(1)
    public String moduleName;
    @Parameterized.Parameter(2)
    public String instanceName;
    @Parameterized.Parameter(3)
    public Map<String, String> additional;

    @Parameterized.Parameters(name = "{index}: parsed({0}) contains moduleName:{1} and instanceName:{2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // With namespaces
                { "/(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)modules/module" +
                        "[{(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)name=instanceName}]" +
                        "[{(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)type=\'moduleType\'}]",
                        "moduleType", "instanceName", null},
                { "/(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)modules/module" +
                        "[{(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)name=instanceName} and " +
                        "{(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)type=moduleType}]",
                        "moduleType", "instanceName", null},
                { "/(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)modules/module" +
                        "[{(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)name=\'instanceName\'} and " +
                        "{(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)type=moduleType}]",
                        "moduleType", "instanceName", null},

                // Without namespaces
                { "/modules/module[name=instanceName][type=moduleType]", "moduleType", "instanceName", null},
                { "/modules/module[type=moduleType][name='instanceName']", "moduleType", "instanceName", null},
                { "/modules/module[name=\'instanceName\'][type=\"moduleType\"]", "moduleType", "instanceName", null},
                { "/modules/module[type=moduleType and name=instanceName]", "moduleType", "instanceName", null},
                { "/modules/module[name=\"instanceName\" and type=moduleType]", "moduleType", "instanceName", null},
                { "/modules/module[type=\"moduleType\" and name=instanceName]", "moduleType", "instanceName", null},
                { "/modules/module[name=\'instanceName\' and type=\"moduleType\"]", "moduleType", "instanceName", null},

                // With inner beans
                { "/modules/module[name=instanceName and type=\"moduleType\"]/inner[key=b]", "moduleType", "instanceName", Collections.singletonMap("inner", "b")},
                { "/modules/module[name=instanceName and type=moduleType]/inner[key=b]", "moduleType", "instanceName", Collections.singletonMap("inner", "b")},
                { "/modules/module[name=instanceName and type=moduleType]/inner[key=\'b\']", "moduleType", "instanceName", Collections.singletonMap("inner", "b")},
                { "/modules/module[name=instanceName and type=moduleType]/inner[key=\"b\"]", "moduleType", "instanceName", Collections.singletonMap("inner", "b")},

                { "/modules/module[name=instanceName and type=moduleType]" +
                        "/(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)inner[(urn:opendaylight:params:xml:ns:yang:controller:config?revision=2013-04-05)key=b]",
                        "moduleType", "instanceName", Collections.singletonMap("inner", "b")},

                { "/modules/module[name=instanceName and type=\"moduleType\"]/inner[key2=a]/inner2[key=b]", "moduleType", "instanceName",
                        new HashMap<String, String>() {{
                            put("inner", "a");
                            put("inner2", "b");
                        }}
                },
        });
    }

    @Test
    public void testFromXpath() throws Exception {
        final RuntimeRpcElementResolved resolved = RuntimeRpcElementResolved.fromXpath(xpath, "element", "namespace");
        assertEquals(moduleName, resolved.getModuleName());
        assertEquals(instanceName, resolved.getInstanceName());
        if (additional != null) {
            assertEquals(additional, resolved.getAdditionalAttributes());
        }
    }
}
