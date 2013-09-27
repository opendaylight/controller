/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.yangtools.yang.model.api.Module;

import com.google.common.collect.Maps;

public class PackageTranslatorTest {
    public static final String EXPECTED_PACKAGE_PREFIX = "org.opendaylight.controller.config";

    @Test
    public void test() throws Exception {
        Map<String, String> map = Maps.newHashMap();
        map.put(ConfigConstants.CONFIG_NAMESPACE, EXPECTED_PACKAGE_PREFIX);
        PackageTranslator tested = new PackageTranslator(map);
        Module module = mock(Module.class);
        doReturn(new URI(ConfigConstants.CONFIG_NAMESPACE + ":threads:api"))
                .when(module).getNamespace();
        assertEquals(EXPECTED_PACKAGE_PREFIX + ".threads.api",
                tested.getPackageName(module));
    }

}
