/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.YangModelSearchUtils;
import org.opendaylight.mdsal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleMXBeanEntryNameConflictTest extends AbstractYangTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleMXBeanEntryNameConflictTest.class);

    public static final String PACKAGE_NAME = "pack2";

    private final List<String> testedModules = ImmutableList.of(
        "config-test-duplicate-attribute-in-list",
        "config-test-duplicate-attribute-in-runtime-and-mxbean",
        "config-test-duplicate-attribute-runtime-bean",
        "config-test-duplicate-attribute",
        "config-test-generated-attributes-name-conflict",
        "config-test-runtime-bean-list-name-conflict2",
        "config-test-runtime-bean-list-name-conflict",
        "config-test-runtime-bean-name-conflict2",
        "config-test-runtime-bean-name-conflict");
    private final Map<String, String> testedYangModulesToExpectedConflictingName = new HashMap<>();

    @Before
    public void setup() {
        testedYangModulesToExpectedConflictingName.put("config-test-duplicate-attribute", "DtoA");
        testedYangModulesToExpectedConflictingName.put("config-test-duplicate-attribute-in-list", "DtoA");
        testedYangModulesToExpectedConflictingName.put("config-test-duplicate-attribute-runtime-bean", "DtoA");
        testedYangModulesToExpectedConflictingName.put("config-test-generated-attributes-name-conflict", "StateB");
        testedYangModulesToExpectedConflictingName.put("config-test-runtime-bean-list-name-conflict",
                "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName.put("config-test-runtime-bean-list-name-conflict2",
                "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName.put("config-test-runtime-bean-name-conflict",
                "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName.put("config-test-runtime-bean-name-conflict2",
                "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName.put("config-test-duplicate-attribute-in-runtime-and-mxbean", "port");
    }

    private Module loadYangs(final String testedModule, final String moduleName) {
        final List<String> yangs = new ArrayList<>();
        yangs.add("/ietf-inet-types.yang");
        yangs.add("/duplicates/" + testedModule + YangConstants.RFC6020_YANG_FILE_EXTENSION);
        yangs.addAll(getConfigApiYangs());

        this.context =  YangParserTestUtils.parseYangResources(ModuleMXBeanEntryNameConflictTest.class, yangs);
        this.namesToModules = YangModelSearchUtils.mapModulesByNames(this.context.getModules());
        this.configModule = this.namesToModules.get(ConfigConstants.CONFIG_MODULE);
        final Module module = this.namesToModules.get(moduleName);
        Preconditions.checkNotNull(module, "Cannot get module %s from %s", moduleName, this.namesToModules.keySet());
        return module;
    }

    @Ignore
    @Test
    public void testNameConflicts() {
        for (final String moduleName : testedModules) {
            final Module testedModule = loadYangs(moduleName, moduleName);

            try {
                LOG.debug("Testing {}", moduleName);
                ModuleMXBeanEntry.create(testedModule, new HashMap<>(), this.context,
                        new TypeProviderWrapper(new TypeProviderImpl(this.context)), PACKAGE_NAME);
                fail(moduleName + " did not cause a name conflict and should");
            } catch (final NameConflictException e) {
                assertEquals(this.testedYangModulesToExpectedConflictingName.get(moduleName), e.getConflictingName());
            }
        }
    }
}
