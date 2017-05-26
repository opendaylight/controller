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
import org.opendaylight.mdsal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
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

        for (final Map.Entry<File, String> currentTestEntry : this.testedFilesToYangModules
                .entrySet()) {
            final String moduleName = currentTestEntry.getValue();
            final File yangFile = currentTestEntry.getKey();
            final Module testedModule = loadYangs(yangFile, moduleName);

            try {
                LOG.debug("Testing {}", yangFile);
                ModuleMXBeanEntry.create(testedModule,
                        new HashMap<>(), this.context,
                        new TypeProviderWrapper(new TypeProviderImpl(this.context)),
                        PACKAGE_NAME);
                fail(yangFile.toString()
                        + " did not cause a name conflict and should");
            } catch (final NameConflictException e) {
                assertEquals(
                        this.testedYangModulesToExpectedConflictingName
                                .get(moduleName),
                        e.getConflictingName());
            }
        }
    }

    private void prepareSamples() {
        final File first = new File(getClass().getResource(
                "/duplicates/config-test-duplicate-attribute-in-list.yang")
                .getFile());
        final File dir = first.getParentFile();

        for (final File testYang : dir.listFiles()) {
            final String moduleName = getYangModuleName(testYang.getName());
            this.testedFilesToYangModules.put(testYang, moduleName);
        }
    }

    private void prepareExceptionAssertions() {
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute", "DtoA");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute-in-list", "DtoA");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute-runtime-bean", "DtoA");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-generated-attributes-name-conflict", "StateB");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-runtime-bean-list-name-conflict",
                "StateARuntimeMXBean");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-runtime-bean-list-name-conflict2",
                "StateARuntimeMXBean");
        this.testedYangModulesToExpectedConflictingName
                .put("config-test-runtime-bean-name-conflict", "StateARuntimeMXBean");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-runtime-bean-name-conflict2",
                "StateARuntimeMXBean");
        this.testedYangModulesToExpectedConflictingName.put(
                "config-test-duplicate-attribute-in-runtime-and-mxbean",
                "port");
    }

    private static String getYangModuleName(final String name) {
        final int startIndex = 0;
        final int endIndex = name.indexOf(".yang");
        return name.substring(startIndex, endIndex);
    }

    private Module loadYangs(final File testedModule, final String moduleName)
            throws Exception {
        final List<InputStream> yangISs = new ArrayList<>();
        yangISs.addAll(getStreams("/ietf-inet-types.yang"));

        yangISs.add(new FileInputStream(testedModule));

        yangISs.addAll(getConfigApiYangInputStreams());

        this.context =  YangParserTestUtils.parseYangStreams(yangISs);
        // close ISs
        for (final InputStream is : yangISs) {
            is.close();
        }
        this.namesToModules = YangModelSearchUtils.mapModulesByNames(this.context
                .getModules());
        this.configModule = this.namesToModules.get(ConfigConstants.CONFIG_MODULE);
        final Module module = this.namesToModules.get(moduleName);
        Preconditions.checkNotNull(module, "Cannot get module %s from %s",
                moduleName, this.namesToModules.keySet());
        return module;
    }

}
