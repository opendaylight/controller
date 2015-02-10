/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntryTest;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.GeneralClassTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.GeneralInterfaceTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method;

public class ModuleMXBeanEntryPluginTest extends ModuleMXBeanEntryTest {

    @Test
    public void testThreadsJavaPlugin() {
        Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = loadThreadsJava();
        {// check threadfactory-naming
            ModuleMXBeanEntry threadFactoryNaming = namesToMBEs
                    .get(THREADFACTORY_NAMING_MXB_NAME);
            Collection<RuntimeBeanEntry> runtimeBeans = threadFactoryNaming
                    .getRuntimeBeans();
            assertThat(runtimeBeans.size(), is(4));
            // first one should be root
            {
                RuntimeBeanEntry rootRB = findFirstByYangName(runtimeBeans,
                        THREADFACTORY_NAMING_MXB_NAME);
                assertThat(rootRB.isRoot(), is(true));
                assertThat(rootRB.getAttributes().size(), is(1));
                JavaAttribute attribute = (JavaAttribute) rootRB
                        .getAttributes().iterator().next();
                assertThat(attribute.getAttributeYangName(),
                        is("created-sessions"));
                assertThat(rootRB.getYangName(),
                        is(THREADFACTORY_NAMING_MXB_NAME));
                Map<String, FtlTemplate> ftlMap = TemplateFactory
                        .getTOAndMXInterfaceFtlFiles(rootRB);
                assertThat(ftlMap.size(), is(1));
                GeneralInterfaceTemplate rootGeneratorInterface = (GeneralInterfaceTemplate) ftlMap
                        .get("NamingThreadFactoryRuntimeMXBean.java");
                assertNotNull(rootGeneratorInterface);
                assertThat(rootGeneratorInterface.getPackageName(),
                        is(PACKAGE_NAME));
                assertThat(rootGeneratorInterface.getFullyQualifiedName(),
                        is(PACKAGE_NAME + ".NamingThreadFactoryRuntimeMXBean"));
                assertThat(
                        rootGeneratorInterface.getTypeDeclaration()
                                .getExtended(),
                        is(Arrays
                                .asList("org.opendaylight.controller.config.api.runtime.RuntimeBean")));

                assertThat(rootGeneratorInterface.getMethods().size(), is(1));
                Method getCreatedSessions = findFirstMethodByName(
                        rootGeneratorInterface.getMethods(),
                        "getCreatedSessions");
                assertThat(getCreatedSessions.getName(),
                        is("getCreatedSessions"));
                assertThat(getCreatedSessions.getParameters().isEmpty(),
                        is(true));
                assertThat(getCreatedSessions.getReturnType(),
                        is(Long.class.getName()));
            }
        }
        {
            ModuleMXBeanEntry threadFactoryNaming = namesToMBEs
                    .get(THREADFACTORY_NAMING_MXB_NAME);
            Collection<RuntimeBeanEntry> runtimeBeans = threadFactoryNaming
                    .getRuntimeBeans();
            assertThat(runtimeBeans.size(), is(4));

            {
                RuntimeBeanEntry streamRB = findFirstByNamePrefix(runtimeBeans,
                        "ThreadStream");
                assertNotNull(streamRB);
                assertFalse(streamRB.getKeyYangName().isPresent());
                assertFalse(streamRB.getKeyJavaName().isPresent());
                Map<String, AttributeIfc> attributeMap = streamRB
                        .getYangPropertiesToTypesMap();
                assertEquals(4, attributeMap.size());
                TOAttribute toAttr = (TOAttribute) attributeMap.get("peer");
                assertNotNull(toAttr);
                JavaAttribute timestampAttr = (JavaAttribute) attributeMap
                        .get("timestamp");
                assertNotNull(timestampAttr);
                JavaAttribute stateAttr = (JavaAttribute) attributeMap
                        .get("state");
                assertNotNull(stateAttr);
                ListAttribute innerStreamList = (ListAttribute) attributeMap
                        .get("inner-stream-list");
                assertNotNull(innerStreamList);

                Map<String, FtlTemplate> ftlMap = TemplateFactory
                        .getTOAndMXInterfaceFtlFiles(streamRB);
                assertThat(ftlMap.size(), is(3));
                GeneralInterfaceTemplate streamGeneralInterface = (GeneralInterfaceTemplate) ftlMap
                        .get("ThreadStreamRuntimeMXBean.java");
                assertThat(streamGeneralInterface.getMethods().size(), is(4));
                Method getPeer = findFirstMethodByName(
                        streamGeneralInterface.getMethods(), "getPeer");
                assertNotNull(getPeer);
                assertThat(getPeer.getReturnType(), is(PACKAGE_NAME + ".Peer"));

                // test TO
                GeneralClassTemplate peerTO = (GeneralClassTemplate) ftlMap
                        .get("pack2.Peer");
                assertThat(peerTO.getPackageName(), is(PACKAGE_NAME));
                assertThat(peerTO.getTypeDeclaration().getExtended().isEmpty(),
                        is(true));
                assertThat(peerTO.getFullyQualifiedName(), is(PACKAGE_NAME
                        + ".Peer"));
                assertThat(peerTO.getMethods().size(), is(5));
                Method getPort = findFirstMethodByName(peerTO.getMethods(),
                        "getPort");
                assertNotNull(getPort);
                Method setPort = findFirstMethodByName(peerTO.getMethods(),
                        "setPort");
                assertNotNull(setPort);
                Method getCoreSize = findFirstMethodByName(peerTO.getMethods(),
                        "getCoreSize");
                Method setCoreSize = findFirstMethodByName(peerTO.getMethods(),
                        "setCoreSize");
                assertNotNull(setCoreSize);
                assertNotNull(getCoreSize);

            }
        }
    }

    private Method findFirstMethodByName(List<? extends Method> methods,
            String name) {
        for (Method ms : methods) {
            if (name.equals(ms.getName())) {
                return ms;
            }
        }
        throw new IllegalArgumentException("Method with given name not found");
    }
}
