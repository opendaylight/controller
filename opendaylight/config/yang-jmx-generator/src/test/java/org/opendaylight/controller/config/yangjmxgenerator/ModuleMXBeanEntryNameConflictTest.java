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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.YangModelSearchUtils;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ModuleMXBeanEntryNameConflictTest extends AbstractYangTest {

    private static final Logger logger = LoggerFactory
            .getLogger(ModuleMXBeanEntryNameConflictTest.class);

    public static final String PACKAGE_NAME = "pack2";
    Map<File, String> testedFilesToYangModules = new HashMap<>();
    Map<String, String> testedYangModulesToExpectedConflictingName = new HashMap<>();

    @Test
    public void testNameConflicts() throws Exception {
        prepareSamples();
        prepareExceptionAssertions();

        for (Map.Entry<File, String> currentTestEntry : testedFilesToYangModules
                .entrySet()) {
            final String moduleName = currentTestEntry.getValue();
            final File yangFile = currentTestEntry.getKey();
            Module testedModule = loadYangs(yangFile, moduleName);

            try {
                logger.debug("Testing {}", yangFile);
                ModuleMXBeanEntry.create(testedModule,
                        new HashMap<QName, ServiceInterfaceEntry>(), context,
                        new TypeProviderWrapper(new TypeProviderImpl(context)),
                        PACKAGE_NAME);
                fail(yangFile.toString()
                        + " did not cause a name conflict and should");
            } catch (NameConflictException e) {
                assertEquals(
                        testedYangModulesToExpectedConflictingName
                                .get(moduleName),
                        e.getConflictingName());
            }
        }
    }

    private void prepareSamples() {
        File first = new File(getClass().getResource(
                "/duplicates/config-test-duplicate-attribute-in-list.yang")
                .getFile());
        File dir = first.getParentFile();

        for (File testYang : dir.listFiles()) {
            String moduleName = getYangModuleName(testYang.getName());
            testedFilesToYangModules.put(testYang, moduleName);
        }
    }

    private void prepareExceptionAssertions() {
        testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute", "DtoA");
        testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute-in-list", "DtoA");
        testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute-runtime-bean", "DtoA");
        testedYangModulesToExpectedConflictingName.put(
                "config-test-generated-attributes-name-conflict", "StateB");
        testedYangModulesToExpectedConflictingName.put(
                "config-test-runtime-bean-list-name-conflict",
                "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName.put(
                "config-test-runtime-bean-list-name-conflict2",
                "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName
                .put("config-test-runtime-bean-name-conflict", "StateARuntimeMXBean");
        testedYangModulesToExpectedConflictingName.put(
                "config-test-runtime-bean-name-conflict2",
                "StateARuntimeMXBean");
    }

    private String getYangModuleName(String name) {
        int startIndex = 0;
        int endIndex = name.indexOf(".yang");
        return name.substring(startIndex, endIndex);
    }

    private Module loadYangs(File testedModule, String moduleName)
            throws Exception {
        List<InputStream> yangISs = new ArrayList<>();
        yangISs.addAll(getStreams("/ietf-inet-types.yang"));

        yangISs.add(new FileInputStream(testedModule));

        yangISs.addAll(getConfigApiYangInputStreams());

        YangParserImpl parser = new YangParserImpl();
        Set<Module> modulesToBuild = parser.parseYangModelsFromStreams(yangISs);
        // close ISs
        for (InputStream is : yangISs) {
            is.close();
        }
        context = parser.resolveSchemaContext(modulesToBuild);
        namesToModules = YangModelSearchUtils.mapModulesByNames(context
                .getModules());
        configModule = namesToModules.get(ConfigConstants.CONFIG_MODULE);
        final Module module = namesToModules.get(moduleName);
        Preconditions.checkNotNull(module, "Cannot get module %s from %s",
                moduleName, namesToModules.keySet());
        return module;
    }

}
