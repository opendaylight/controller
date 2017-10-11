/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.unknownextension;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntryTest;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.YangModelSearchUtils;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class UnknownExtensionTest extends ServiceInterfaceEntryTest {

    @Test
    public void testStopOnUnknownLanguageExtension() throws Exception {
        List<String> yangs = new ArrayList<>();
        yangs.addAll(getConfigApiYangs());
        yangs.add("test-ifcWithUnknownExtension.yang");

        context = YangParserTestUtils.parseYangResources(UnknownExtensionTest.class, yangs);
        namesToModules = YangModelSearchUtils.mapModulesByNames(context.getModules());
        configModule = namesToModules.get(ConfigConstants.CONFIG_MODULE);
        threadsModule = namesToModules.get(ConfigConstants.CONFIG_THREADS_MODULE);
        try {
            super.testCreateFromIdentities();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(),
                e.getMessage().startsWith("Unexpected unknown schema node."));
        }
    }

}
