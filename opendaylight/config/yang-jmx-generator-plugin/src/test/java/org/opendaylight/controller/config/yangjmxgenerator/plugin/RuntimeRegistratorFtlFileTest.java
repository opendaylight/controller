/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeRegistratorTest;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.RuntimeRegistratorFtlTemplate;

public class RuntimeRegistratorFtlFileTest extends RuntimeRegistratorTest {

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

        assertThat(createdFtls.values().size(), is(2));
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
        assertThat(createdFtls.values().size(), is(4));
    }
}
