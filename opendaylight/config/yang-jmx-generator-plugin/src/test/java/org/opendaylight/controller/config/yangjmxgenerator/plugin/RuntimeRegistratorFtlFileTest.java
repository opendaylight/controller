/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeRegistratorTest;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlFilePersister;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.RuntimeRegistratorFtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FormattingUtil;

public class RuntimeRegistratorFtlFileTest extends RuntimeRegistratorTest {
    private final FtlFilePersister ftlFilePersister = new FtlFilePersister();

    @Test
    public void testRootWithoutAnything() {
        RuntimeBeanEntry rootRB = prepareRootRB(Collections
                .<RuntimeBeanEntry> emptyList());
        Map<String, FtlTemplate> createdFtls = RuntimeRegistratorFtlTemplate
                .create(rootRB);
        assertThat(createdFtls.size(), is(2));
        String rootRegistratorName = RuntimeRegistratorFtlTemplate
                .getJavaNameOfRuntimeRegistration(rootRB.getJavaNamePrefix());
        FtlTemplate rootFtlFile = createdFtls.get(rootRegistratorName);
        assertNotNull(rootFtlFile);

        Map<FtlTemplate, String> serializedFtls = ftlFilePersister
                .serializeFtls(createdFtls.values());
        assertThat(serializedFtls.size(), is(2));
    }

    @Test
    public void testHierarchy2() {
        RuntimeBeanEntry grandChildRB = prepareChildRB(
                Collections.<RuntimeBeanEntry> emptyList(), "grand");
        RuntimeBeanEntry childRB = prepareChildRB(Arrays.asList(grandChildRB),
                "");
        RuntimeBeanEntry rootRB = prepareRootRB(Arrays.asList(childRB));

        Map<String, FtlTemplate> createdFtls = RuntimeRegistratorFtlTemplate
                .create(rootRB);
        Map<FtlTemplate, String> serializedFtls = ftlFilePersister
                .serializeFtls(createdFtls.values());
        assertThat(serializedFtls.size(), is(4));

        assertThat(
                findRegistrationOutput(createdFtls, grandChildRB,
                        serializedFtls), not(containsString(" register(")));

        FtlTemplate registrator = createdFtls.get(RuntimeRegistratorFtlTemplate
                .getJavaNameOfRuntimeRegistrator(rootRB));
        FormattingUtil.cleanUpEmptyLinesAndIndent(serializedFtls
                .get(registrator));

    }

    private String findRegistrationOutput(Map<String, FtlTemplate> createdFtls,
            RuntimeBeanEntry rb, Map<FtlTemplate, String> serializedFtls) {
        RuntimeRegistratorFtlTemplate rbFtlFile = (RuntimeRegistratorFtlTemplate) createdFtls
                .get(RuntimeRegistratorFtlTemplate.getJavaNameOfRuntimeRegistration(rb.getJavaNamePrefix()));
        assertNotNull(rbFtlFile);
        String unformatted = serializedFtls.get(rbFtlFile);
        assertNotNull(unformatted);
        return FormattingUtil.cleanUpEmptyLinesAndIndent(unformatted);
    }

}
