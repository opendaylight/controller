/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import com.google.common.collect.Sets;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TypedAttribute;
import org.opendaylight.yangtools.binding.generator.util.Types;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;

public class ModuleMXBeanEntryTest extends AbstractYangTest {

    public static final String PACKAGE_NAME = "pack2";

    protected static final URI THREADS_NAMESPACE;
    protected static final Date THREADS_REVISION_DATE;

    static {
        try {
            THREADS_NAMESPACE = new URI(ConfigConstants.CONFIG_NAMESPACE
                    + ":threads");
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
        SimpleDateFormat revisionFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            THREADS_REVISION_DATE = revisionFormat.parse("2013-04-09");
        } catch (ParseException e) {
            throw new Error(e);
        }
    }

    protected Map<QName, ServiceInterfaceEntry> modulesToSIEs;


    @Before
    public void setUp() {
        modulesToSIEs = loadThreadsServiceInterfaceEntries("packages.sis");
    }


    protected Map<String /* identity local name */, ModuleMXBeanEntry> loadThreadsJava() {
        return loadThreadsJava(modulesToSIEs, PACKAGE_NAME);
    }

    @Test
    public void test_jmxImplModule() {
        Map<IdentitySchemaNode, ServiceInterfaceEntry> identitiesToSIs = new HashMap<>();
        Map<QName, ServiceInterfaceEntry> modulesToSIEs = ServiceInterfaceEntry
                .create(threadsModule, PACKAGE_NAME,identitiesToSIs);
        modulesToSIEs.putAll(ServiceInterfaceEntry.create(jmxModule,
                PACKAGE_NAME,identitiesToSIs));
        Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                .create(jmxImplModule, modulesToSIEs, context, new TypeProviderWrapper(new TypeProviderImpl(context))
                , PACKAGE_NAME);
        Map<String, AttributeIfc> attributes = namesToMBEs.get("impl-netconf")
                .getAttributes();

        assertCorrectAttributesSize(namesToMBEs, attributes);

        //
        DependencyAttribute threadFactoryAttribute = (DependencyAttribute) attributes
                .get("thread-factory");
        assertNotNull(threadFactoryAttribute);
        assertFalse(threadFactoryAttribute.getDependency().isMandatory());
        assertThat(threadFactoryAttribute.getDependency().getSie()
                .getTypeName(), is("ThreadFactoryServiceInterface"));
        assertThat(threadFactoryAttribute.getAttributeYangName(),
                is("thread-factory"));
        assertThat(threadFactoryAttribute.getLowerCaseCammelCase(),
                is("threadFactory"));
        assertThat(threadFactoryAttribute.getUpperCaseCammelCase(),
                is("ThreadFactory"));
        assertThat(threadFactoryAttribute.getOpenType(), isA(SimpleType.class));
        assertNull(threadFactoryAttribute.getNullableDefault());
        assertNull(threadFactoryAttribute.getNullableDescription());
        assertThat(threadFactoryAttribute.getType().getName(), is("ObjectName"));
    }

    private void assertCorrectAttributesSize(final Map<String, ModuleMXBeanEntry> namesToMBEs, final Map<String, AttributeIfc> attributes) {
        assertEquals(14, attributes.size());
        assertEquals(1, namesToMBEs.get("impl-netconf").getRuntimeBeans().size());
        assertEquals(2, namesToMBEs.get("impl-netconf").getRuntimeBeans().iterator().next().getAttributes().size());

        assertEquals(4, namesToMBEs.get("impl").getAttributes().size());
        assertEquals(1, namesToMBEs.get("impl").getRuntimeBeans().size());
        assertEquals(1, namesToMBEs.get("impl").getRuntimeBeans().iterator().next().getAttributes().size());
    }

    protected RuntimeBeanEntry findFirstByYangName(
            final Collection<RuntimeBeanEntry> runtimeBeans, final String yangName) {
        for (RuntimeBeanEntry rb : runtimeBeans) {
            if (yangName.equals(rb.getYangName())) {
                return rb;
            }
        }
        throw new IllegalArgumentException("Yang name not found:" + yangName
                + " in " + runtimeBeans);
    }

    @Test
    public void testGetWhenConditionMatcher() {
        assertMatches("config",
                "/config:modules/config:module/config:type = 'threadpool-dynamic'");
        assertMatches("ns",
                "/ns:modules/ns:module/ns:type = 'threadpool-dynamic'");
        assertMatches("config",
                "/config:modules/config:module/config:type=\"threadpool-dynamic\"");
    }

    private void assertMatches(final String prefix, final String input) {
        RevisionAwareXPath whenConstraint = mock(RevisionAwareXPath.class);
        doReturn(input).when(whenConstraint).toString();
        Matcher output = ModuleMXBeanEntryBuilder.getWhenConditionMatcher(prefix,
                whenConstraint);
        assertTrue(output.matches());
        assertEquals("threadpool-dynamic", output.group(1));
    }

    @Test
    public void testThreadsJava() {
        Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = loadThreadsJava();

        { // check threadpool-dynamic
            ModuleMXBeanEntry dynamicThreadPool = namesToMBEs
                    .get(THREADPOOL_DYNAMIC_MXB_NAME);
            Map<String, AttributeIfc> attributes = dynamicThreadPool
                    .getAttributes();
            // core-size, keepalive, maximum-size
            // threadfactory
            Set<String> longAttribs = Sets.newHashSet("core-size",
                    "maximum-size");
            for (String longAttrib : longAttribs) {

                TypedAttribute attribute = (TypedAttribute) attributes
                        .get(longAttrib);
                assertThat("Failed to check " + longAttrib,
                        attribute.getType(),
                        is((Type) Types.typeForClass(Long.class)));
            }
            // check dependency on thread factory
            QName threadfactoryQName = QName.create(THREADS_NAMESPACE,
                    THREADS_REVISION_DATE, "threadfactory");
            ServiceInterfaceEntry threadFactorySIEntry = modulesToSIEs
                    .get(threadfactoryQName);
            assertNotNull(threadFactorySIEntry);
            boolean expectedMandatory = true;
            TypedAttribute actualThreadFactory = (TypedAttribute) attributes
                    .get("threadfactory");

            DataSchemaNode mockedDataSchemaNode = mock(DataSchemaNode.class);
            doReturn(Collections.emptyList()).when(mockedDataSchemaNode)
            .getUnknownSchemaNodes();
            doReturn(threadfactoryQName).when(mockedDataSchemaNode).getQName();
            AttributeIfc expectedDependencyAttribute = new DependencyAttribute(
                    mockedDataSchemaNode, threadFactorySIEntry,
                    expectedMandatory, "threadfactory description");
            assertThat(actualThreadFactory, is(expectedDependencyAttribute));
            assertThat(
                    dynamicThreadPool
                    .getFullyQualifiedName("DynamicThreadPoolModuleMXBean"),
                    is(PACKAGE_NAME + ".DynamicThreadPoolModuleMXBean"));
            assertThat(dynamicThreadPool.getNullableDescription(),
                    is("threadpool-dynamic description"));
            assertThat(dynamicThreadPool.getYangModuleName(),
                    is("config-threads-java"));
            assertThat(dynamicThreadPool.getYangModuleLocalname(),
                    is(THREADPOOL_DYNAMIC_MXB_NAME));

            // check root runtime bean
            Collection<RuntimeBeanEntry> runtimeBeans = dynamicThreadPool
                    .getRuntimeBeans();
            assertThat(runtimeBeans.size(), is(1));
            RuntimeBeanEntry rootRB = findFirstByYangName(runtimeBeans,
                    THREADPOOL_DYNAMIC_MXB_NAME);
            assertThat(rootRB.isRoot(), is(true));
            assertThat(rootRB.getAttributes().size(), is(1));
            JavaAttribute attribute = (JavaAttribute) rootRB.getAttributes()
                    .iterator().next();
            assertThat(attribute.getAttributeYangName(), is("created-sessions"));
            assertThat(rootRB.getYangName(), is(THREADPOOL_DYNAMIC_MXB_NAME));
            assertThat(attribute.getType().getFullyQualifiedName(),
                    is(Long.class.getName()));
        }
        {// check threadfactory-naming
            ModuleMXBeanEntry threadFactoryNaming = namesToMBEs
                    .get(THREADFACTORY_NAMING_MXB_NAME);
            Collection<RuntimeBeanEntry> runtimeBeans = threadFactoryNaming
                    .getRuntimeBeans();
            assertThat(runtimeBeans.size(), is(4));
            {
                RuntimeBeanEntry threadRB = findFirstByYangName(runtimeBeans,
                        "thread");
                assertNotNull(threadRB);
                assertFalse(threadRB.isRoot());
                assertEquals("name", threadRB.getKeyYangName().get());
                assertEquals("Name", threadRB.getKeyJavaName().get());
                assertThat(threadRB.getAttributes().size(), is(1));
                AttributeIfc threadNameAttr = threadRB.getAttributes()
                        .iterator().next();
                assertThat(threadNameAttr.getAttributeYangName(), is("name"));
                assertTrue(threadNameAttr instanceof JavaAttribute);
                assertThat(((JavaAttribute) threadNameAttr).getType()
                        .getFullyQualifiedName(), is(String.class.getName()));
                assertThat(threadRB.getRpcs().size(), is(2));
            }
            {
                RuntimeBeanEntry streamRB = findFirstByYangName(runtimeBeans,
                        "stream");
                assertNotNull(streamRB);
                assertFalse(streamRB.getKeyYangName().isPresent());
                assertFalse(streamRB.getKeyJavaName().isPresent());
                Map<String, AttributeIfc> attributeMap = streamRB
                        .getYangPropertiesToTypesMap();
                assertEquals(4, attributeMap.size());

                TOAttribute toAttr = (TOAttribute) attributeMap.get("peer");
                assertNotNull(toAttr);
                assertThat(toAttr.getAttributeYangName(), is("peer"));
                assertThat(toAttr.getLowerCaseCammelCase(), is("peer"));
                assertThat(toAttr.getUpperCaseCammelCase(), is("Peer"));
                assertThat(toAttr.getOpenType(), isA(CompositeType.class));
                Set<String> propsExpected = new HashSet<String>(2);
                propsExpected.add("port");
                propsExpected.add("core-size");
                assertThat(toAttr.getYangPropertiesToTypesMap().keySet(),
                        is(propsExpected));
                propsExpected = new HashSet<String>(2);
                propsExpected.add("Port");
                propsExpected.add("CoreSize");
                assertThat(
                        toAttr.getCapitalizedPropertiesToTypesMap().keySet(),
                        is(propsExpected));
                propsExpected = new HashSet<String>(2);
                propsExpected.add("port");
                propsExpected.add("coreSize");
                assertThat(toAttr.getJmxPropertiesToTypesMap().keySet(),
                        is(propsExpected));

                JavaAttribute timestampAttr = (JavaAttribute) attributeMap
                        .get("timestamp");
                assertNotNull(timestampAttr);

                JavaAttribute stateAttr = (JavaAttribute) attributeMap
                        .get("state");
                assertNotNull(stateAttr);

                ListAttribute innerStream = (ListAttribute) attributeMap
                        .get("inner-stream-list");
                assertNotNull(innerStream);
                assertThat(innerStream.getAttributeYangName(),
                        is("inner-stream-list"));
                assertThat(innerStream.getLowerCaseCammelCase(),
                        is("innerStreamList"));
                assertThat(innerStream.getUpperCaseCammelCase(),
                        is("InnerStreamList"));
                assertThat(innerStream.getOpenType(), isA(ArrayType.class));

            }

        }
        { // test multiple dependencies
            ModuleMXBeanEntry threadPoolRegistry = namesToMBEs.get(THREADPOOL_REGISTRY_IMPL_NAME);
            Map<String, AttributeIfc> attributes = threadPoolRegistry.getAttributes();
            assertEquals(1, attributes.size());
            AttributeIfc threadpoolsAttr = attributes.get("threadpools");
            assertNotNull(threadpoolsAttr);
            assertTrue(threadpoolsAttr instanceof ListDependenciesAttribute);
            ListDependenciesAttribute threadpools = (ListDependenciesAttribute) threadpoolsAttr;
        }
    }

}
