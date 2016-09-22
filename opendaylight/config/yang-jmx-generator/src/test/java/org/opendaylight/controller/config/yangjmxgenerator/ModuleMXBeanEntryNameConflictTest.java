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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.YangModelSearchUtils;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleMXBeanEntryNameConflictTest extends AbstractYangTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(ModuleMXBeanEntryNameConflictTest.class);

    public static final String PACKAGE_NAME = "pack2";
    Map<File, String> testedFilesToYangModules = new HashMap<>();
    Map<String, String> testedYangModulesToExpectedConflictingName = new HashMap<>();

    @Ignore
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
                LOG.debug("Testing {}", yangFile);
                ModuleMXBeanEntry.create(testedModule,
                        new HashMap<>(), context,
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
        testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute-in-runtime-and-mxbean",
                "port");
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

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        context = reactor.buildEffective(yangISs);
        // close ISs
        for (InputStream is : yangISs) {
            is.close();
        }
        namesToModules = YangModelSearchUtils.mapModulesByNames(context
                .getModules());
        configModule = namesToModules.get(ConfigConstants.CONFIG_MODULE);
        final Module module = namesToModules.get(moduleName);
        Preconditions.checkNotNull(module, "Cannot get module %s from %s",
                moduleName, namesToModules.keySet());
        return module;
    }

}
