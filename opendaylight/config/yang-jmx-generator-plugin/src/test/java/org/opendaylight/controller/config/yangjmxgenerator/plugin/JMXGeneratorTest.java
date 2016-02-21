/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.config.yangjmxgenerator.PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.AbstractModule;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntryTest;
import org.osgi.framework.BundleContext;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

//TODO: refactor
public class JMXGeneratorTest extends AbstractGeneratorTest {

    JMXGenerator jmxGenerator;

    protected final HashMap<String, String> map = new HashMap<>();
    protected File outputBaseDir;
    File generatedResourcesDir;

    private static final List<String> expectedModuleFileNames = ServiceInterfaceEntryTest
            .toFileNames("[AbstractAsyncEventBusModule.java, AbstractAsyncEventBusModuleFactory.java, AbstractDynamicThreadPoolModule.java, AbstractDynamicThreadPoolModuleFactory.java, AbstractEventBusModule.java, AbstractEventBusModuleFactory.java, AbstractNamingThreadFactoryModule.java, AbstractNamingThreadFactoryModuleFactory.java, AbstractThreadPoolRegistryImplModule.java, AbstractThreadPoolRegistryImplModuleFactory.java, AsyncEventBusModule.java, AsyncEventBusModuleFactory.java, AsyncEventBusModuleMXBean.java, AsyncEventBusRuntimeMXBean.java, AsyncEventBusRuntimeRegistration.java, AsyncEventBusRuntimeRegistrator.java, DynamicThreadPoolModule.java, DynamicThreadPoolModuleFactory.java, DynamicThreadPoolModuleMXBean.java, DynamicThreadPoolRuntimeMXBean.java, DynamicThreadPoolRuntimeRegistration.java, DynamicThreadPoolRuntimeRegistrator.java, EventBusModule.java, EventBusModuleFactory.java, EventBusModuleMXBean.java, EventRuntimeMXBean.java, EventRuntimeRegistration.java, FromGrouping.java, InnerStreamList.java, NamingThreadFactoryModule.java, NamingThreadFactoryModuleFactory.java, NamingThreadFactoryModuleMXBean.java, NamingThreadFactoryRuntimeMXBean.java, NamingThreadFactoryRuntimeRegistration.java, NamingThreadFactoryRuntimeRegistrator.java, Peer.java, StreamRuntimeMXBean.java, StreamRuntimeRegistration.java, ThreadPoolRegistryImplModule.java, ThreadPoolRegistryImplModuleFactory.java, ThreadPoolRegistryImplModuleMXBean.java, ThreadRuntimeMXBean.java, ThreadRuntimeRegistration.java, ThreadStreamRuntimeMXBean.java, ThreadStreamRuntimeRegistration.java]");

    private static final List<String> expectedBGPNames = ServiceInterfaceEntryTest
            .toFileNames("[AbstractBgpListenerImplModule.java, " + "AbstractBgpListenerImplModuleFactory.java, " +
                    "BgpListenerImplModule.java, " + "BgpListenerImplModuleFactory.java, " +
                    "BgpListenerImplModuleMXBean.java, Peers.java]");

    private static final List<String> expectedNetconfNames = ServiceInterfaceEntryTest
            .toFileNames("[AbstractNetconfTestImplModule.java, " + "AbstractNetconfTestImplModuleFactory.java, " +
                    "AbstractTestImplModule.java, " + "AbstractTestImplModuleFactory.java, " +
                    "AutoCloseableServiceInterface.java, " + "ComplexDtoBInner.java, ComplexList.java, Deep.java, " +
                    "DtoA.java, DtoA1.java, DtoAInner.java, DtoAInnerInner.java, DtoB.java, DtoC.java," + "NetconfTestImplModule.java, NetconfTestImplModuleFactory.java," + "NetconfTestImplModuleMXBean.java, Peer.java, SimpleList.java, TestImplModule.java, " + "TestImplModuleFactory.java," + " TestImplModuleMXBean.java" + "]");
    private static final List<String> expectedTestFiles = ServiceInterfaceEntryTest
            .toFileNames("[AbstractNetconfTestFileImplModule.java, AbstractNetconfTestFileImplModuleFactory.java, " +
                    "AbstractNetconfTestFiles1ImplModule.java, AbstractNetconfTestFiles1ImplModuleFactory.java, " +
                    "AbstractTestFileImplModule.java, AbstractTestFileImplModuleFactory.java, " +
                    "AbstractTestFiles1ImplModule.java, AbstractTestFiles1ImplModuleFactory.java, DtoA.java, " +
                    "DtoA.java, NetconfTestFileImplModuleMXBean.java, NetconfTestFileImplRuntimeMXBean.java, " +
                    "NetconfTestFileImplRuntimeRegistration.java, NetconfTestFileImplRuntimeRegistrator.java, " +
                    "NetconfTestFiles1ImplModule.java, NetconfTestFiles1ImplModuleFactory.java, " +
                    "NetconfTestFiles1ImplModuleMXBean.java, NetconfTestFiles1ImplRuntimeMXBean.java, " +
                    "NetconfTestFiles1ImplRuntimeRegistration.java, NetconfTestFiles1ImplRuntimeRegistrator.java, TestFileImplModule.java, TestFileImplModuleFactory.java, TestFileImplModuleMXBean.java, TestFileImplRuntimeMXBean.java, TestFileImplRuntimeRegistration.java, TestFileImplRuntimeRegistrator.java, TestFiles1ImplModule.java, TestFiles1ImplModuleFactory.java, TestFiles1ImplModuleMXBean.java, TestFiles1ImplRuntimeMXBean.java, TestFiles1ImplRuntimeRegistration.java, TestFiles1ImplRuntimeRegistrator.java]");
    private static final List<String> expectedAllFileNames = ServiceInterfaceEntryTest
            .toFileNames("[AbstractAsyncEventBusModule.java, AbstractAsyncEventBusModuleFactory.java, AbstractBgpListenerImplModule.java, AbstractBgpListenerImplModuleFactory.java, AbstractDynamicThreadPoolModule.java, AbstractDynamicThreadPoolModuleFactory.java, AbstractEventBusModule.java, AbstractEventBusModuleFactory.java, AbstractNamingThreadFactoryModule.java, AbstractNamingThreadFactoryModuleFactory.java, AbstractNetconfTestFileImplModule.java, AbstractNetconfTestFileImplModuleFactory.java, AbstractNetconfTestFiles1ImplModule.java, AbstractNetconfTestFiles1ImplModuleFactory.java, AbstractNetconfTestImplModule.java, AbstractNetconfTestImplModuleFactory.java, AbstractTestFileImplModule.java, AbstractTestFileImplModuleFactory.java, AbstractTestFiles1ImplModule.java, AbstractTestFiles1ImplModuleFactory.java, AbstractTestImplModule.java, AbstractTestImplModuleFactory.java, AbstractThreadPoolRegistryImplModule.java, AbstractThreadPoolRegistryImplModuleFactory.java, AsyncEventBusModule.java, AsyncEventBusModuleFactory.java, AsyncEventBusModuleMXBean.java, AsyncEventBusRuntimeMXBean.java, AsyncEventBusRuntimeRegistration.java, AsyncEventBusRuntimeRegistrator.java, AutoCloseableServiceInterface.java, BgpListenerImplModule.java, BgpListenerImplModuleFactory.java, BgpListenerImplModuleMXBean.java, BgpListenerImplRuntimeMXBean.java, BgpListenerImplRuntimeRegistration.java, BgpListenerImplRuntimeRegistrator.java, ComplexDtoBInner.java, ComplexList.java, Deep.java, DtoA.java, DtoA.java, DtoA.java, DtoA1.java, DtoAInner.java, DtoAInnerInner.java, DtoB.java, DtoC.java, DynamicThreadPoolModule.java, DynamicThreadPoolModuleFactory.java, DynamicThreadPoolModuleMXBean.java, DynamicThreadPoolRuntimeMXBean.java, DynamicThreadPoolRuntimeRegistration.java, DynamicThreadPoolRuntimeRegistrator.java, EventBusModule.java, EventBusModuleFactory.java, EventBusModuleMXBean.java, EventBusServiceInterface.java, EventRuntimeMXBean.java, EventRuntimeRegistration.java, FromGrouping.java, InnerStreamList.java, NamingThreadFactoryModule.java, NamingThreadFactoryModuleFactory.java, NamingThreadFactoryModuleMXBean.java, NamingThreadFactoryRuntimeMXBean.java, NamingThreadFactoryRuntimeRegistration.java, NamingThreadFactoryRuntimeRegistrator.java, NetconfTestFileImplModule.java, NetconfTestFileImplModuleFactory.java, NetconfTestFileImplModuleMXBean.java, NetconfTestFileImplRuntimeMXBean.java, NetconfTestFileImplRuntimeRegistration.java, NetconfTestFileImplRuntimeRegistrator.java, NetconfTestFiles1ImplModule.java, NetconfTestFiles1ImplModuleFactory.java, NetconfTestFiles1ImplModuleMXBean.java, NetconfTestFiles1ImplRuntimeMXBean.java, NetconfTestFiles1ImplRuntimeRegistration.java, NetconfTestFiles1ImplRuntimeRegistrator.java, NetconfTestImplModule.java, NetconfTestImplModuleFactory.java, NetconfTestImplModuleMXBean.java, NetconfTestImplRuntimeMXBean.java, NetconfTestImplRuntimeRegistration.java, NetconfTestImplRuntimeRegistrator.java, Peer.java, Peer.java, PeersRuntimeMXBean.java, PeersRuntimeRegistration.java, ScheduledThreadPoolServiceInterface.java, SimpleList.java, StreamRuntimeMXBean.java, StreamRuntimeRegistration.java, TestFileImplModule.java, TestFileImplModuleFactory.java, TestFileImplModuleMXBean.java, TestFileImplRuntimeMXBean.java, TestFileImplRuntimeRegistration.java, TestFileImplRuntimeRegistrator.java, TestFiles1ImplModule.java, TestFiles1ImplModuleFactory.java, TestFiles1ImplModuleMXBean.java, TestFiles1ImplRuntimeMXBean.java, TestFiles1ImplRuntimeRegistration.java, TestFiles1ImplRuntimeRegistrator.java, TestImplModule.java, TestImplModuleFactory.java, TestImplModuleMXBean.java, TestImplRuntimeMXBean.java, TestImplRuntimeRegistration.java, TestImplRuntimeRegistrator.java, ThreadFactoryServiceInterface.java, ThreadPoolRegistryImplModule.java, ThreadPoolRegistryImplModuleFactory.java, ThreadPoolRegistryImplModuleMXBean.java, ThreadPoolServiceInterface.java, ThreadRuntimeMXBean.java, ThreadRuntimeRegistration.java, ThreadStreamRuntimeMXBean.java, ThreadStreamRuntimeRegistration.java]");
    private static final List<String> expectedGenerateMBEsListNames = ServiceInterfaceEntryTest
            .toFileNames("[AbstractBgpListenerImplModule.java, AbstractBgpListenerImplModuleFactory.java, BgpListenerImplModule.java, BgpListenerImplModuleFactory.java, BgpListenerImplModuleMXBean.java, BgpListenerImplRuntimeMXBean.java, BgpListenerImplRuntimeRegistration.java, BgpListenerImplRuntimeRegistrator.java, PeersRuntimeMXBean.java, PeersRuntimeRegistration.java]");

    @Before
    public void setUp() {
        map.put(JMXGenerator.NAMESPACE_TO_PACKAGE_PREFIX + "1",
                ConfigConstants.CONFIG_NAMESPACE + JMXGenerator.NAMESPACE_TO_PACKAGE_DIVIDER + EXPECTED_PACKAGE_PREFIX);
        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "false");
        jmxGenerator = new JMXGenerator(new CodeWriter());
        jmxGenerator.setAdditionalConfig(map);
        File targetDir = new File(generatorOutputPath, "target");
        generatedResourcesDir = new File(targetDir, "generated-resources");
        jmxGenerator.setResourceBaseDir(generatedResourcesDir);
        MavenProject project = mock(MavenProject.class);
        doReturn(generatorOutputPath).when(project).getBasedir();
        jmxGenerator.setMavenProject(project);
        outputBaseDir = JMXGenerator.concatFolders(targetDir, "generated-sources", "config");
    }

    @Test
    public void generateSIsMBsTest() {
        Collection<File> files = jmxGenerator.generateSources(context,
                outputBaseDir, context.getModules());
        List<String> expectedFileNames = new ArrayList<>();
        expectedFileNames
                .addAll(ServiceInterfaceEntryTest.expectedSIEFileNames);
        expectedFileNames.addAll(expectedModuleFileNames);

        expectedFileNames.addAll(expectedBGPNames);
        expectedFileNames.addAll(expectedNetconfNames);
        expectedFileNames.addAll(expectedTestFiles);
        Collections.sort(expectedFileNames);
        // TODO: separate expectedAllFileNames into expectedBGPNames,
        // expectedNetconfNames
        assertEquals(expectedAllFileNames, toFileNames(files));

        verifyModuleFactoryFile(false);
    }

    private void verifyModuleFactoryFile(final boolean shouldBePresent) {
        File factoryFile = new File(generatedResourcesDir, "META-INF"
                + File.separator + "services" + File.separator
                + ModuleFactory.class.getName());
        if (!shouldBePresent) {
            assertFalse("Factory file should not be generated",
                    factoryFile.exists());
        } else {
            assertTrue("Factory file should be generated", factoryFile.exists());
        }
    }

    private static List<String> toFileNames(final Collection<File> files) {
        List<String> result = new ArrayList<>();
        for (File f : files) {
            result.add(f.getName());
        }
        Collections.sort(result);
        return result;
    }

    @Test
    public void generateSIEsTest() throws IOException, ParseException {
        Collection<File> files = jmxGenerator.generateSources(context,
                outputBaseDir, Collections.singleton(threadsModule));
        assertEquals(ServiceInterfaceEntryTest.expectedSIEFileNames, toFileNames(files));

        for (File file : files) {
            String fileName = file.getName();
            SieASTVisitor verifier = new SieASTVisitor(EXPECTED_PACKAGE_PREFIX + ".threads", fileName);
            verifyFile(file, verifier);

            assertThat(verifier.extnds,
                    containsString("org.opendaylight.controller.config.api.annotations.AbstractServiceInterface"));
            assertNotNull(verifier.javadoc);

            switch (fileName) {
            case "ThreadPoolServiceInterface.java":
                assertContains(verifier.descriptionAnotValue, "A simple pool of threads able to execute work.");
                assertContains(verifier.sieAnnotValue, "threadpool");
                assertContains(verifier.sieAnnotOsgiRegistrationType, EXPECTED_PACKAGE_PREFIX + ".threadpool.ThreadPool.class");
                break;
            case "ScheduledThreadPoolServiceInterface.java":
                assertContains(verifier.extnds,
                        EXPECTED_PACKAGE_PREFIX  + ".threads.ThreadPoolServiceInterface");
                assertContains(verifier.descriptionAnotValue,
                        "An extension of the simple pool of threads able to schedule work to be executed at some point in time.");
                assertContains(verifier.sieAnnotValue, "scheduled-threadpool");
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        EXPECTED_PACKAGE_PREFIX + ".threadpool.ScheduledThreadPool.class");
                break;
            case "EventBusServiceInterface.java":
                assertContains(verifier.descriptionAnotValue,
                        "Service representing an event bus. The service acts as message router between event producers and event consumers");
                assertContains(verifier.sieAnnotValue, "eventbus");
                assertContains(verifier.sieAnnotOsgiRegistrationType, "com.google.common.eventbus.EventBus.class");
                break;
            case "ThreadFactoryServiceInterface.java":
                assertContains( verifier.descriptionAnotValue,
                        "Service representing a ThreadFactory instance. It is directly useful in Java world, where various library pieces need to create threads and you may want to inject a customized thread implementation.");
                assertContains(verifier.sieAnnotValue, "threadfactory");
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        "java.util.concurrent.ThreadFactory.class");
                break;
            case "ScheduledExecutorServiceServiceInterface.java":
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        "java.util.concurrent.ScheduledExecutorService.class");
                break;
            default:
                fail("Unknown generated sie " + fileName);
            }
        }
    }

    @Test
    public void generateMBEsListTest() throws Exception {
        // default value for module factory file is true
        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "randomValue");
        jmxGenerator.setAdditionalConfig(map);

        Collection<File> files = jmxGenerator.generateSources(context, outputBaseDir,
            Collections.singleton(bgpListenerJavaModule));

        assertEquals(expectedGenerateMBEsListNames, toFileNames(files));
    }

    @Test
    public void generateMBEsTest() throws Exception {
        // default value for module factory file is true
        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "randomValue");
        jmxGenerator.setAdditionalConfig(map);

        Collection<File> files = jmxGenerator.generateSources(context, outputBaseDir,
            Collections.singleton(threadsJavaModule));

        assertEquals(expectedModuleFileNames, toFileNames(files));

        for (File file : files) {
            final String name = file.getName();
            if (!name.endsWith("java")) {
                continue;
            }

            MbeASTVisitor visitor = new MbeASTVisitor(EXPECTED_PACKAGE_PREFIX + ".threads.java", name);

            verifyFile(file, visitor);

            switch (name) {
            case "AbstractDynamicThreadPoolModule.java":
                assertAbstractDynamicThreadPoolModule(visitor);
                break;
            case "AsyncEventBusModuleMXBean.java":
                assertEquals("Incorrenct number of generated methods", 4, visitor.methods.size());
                break;
            case "AbstractNamingThreadFactoryModuleFactory.java":
                assertAbstractNamingThreadFactoryModuleFactory(visitor);
                break;
            case "AsyncEventBusModule.java":
                assertContains(visitor.extnds, EXPECTED_PACKAGE_PREFIX + ".threads.java.AbstractAsyncEventBusModule");
                visitor.assertFields(0);
                assertEquals("Incorrenct number of generated methods", 2, visitor.methods.size());
                visitor.assertConstructors(2);
                visitor.assertMethodDescriptions(0);
                visitor.assertMethodJavadocs(0);
                break;
            case "EventBusModuleFactory.java":
                assertContains(visitor.extnds,
                    EXPECTED_PACKAGE_PREFIX + ".threads.java.AbstractEventBusModuleFactory");
                visitor.assertFields(0);
                assertEquals("Incorrenct number of generated methods", 0, visitor.methods.size());
                visitor.assertConstructors(0);
                visitor.assertMethodDescriptions(0);
                visitor.assertMethodJavadocs(0);
                break;
            }
        }

        verifyXmlFiles(Collections2.filter(files, new Predicate<File>() {
            @Override
            public boolean apply(final File input) {
                return input.getName().endsWith("xml");
            }
        }));

        // verify ModuleFactory file
        File moduleFactoryFile = JMXGenerator.concatFolders(generatedResourcesDir, "META-INF", "services",
                ModuleFactory.class.getName());
        assertTrue(moduleFactoryFile.exists());
        Set<String> lines = ImmutableSet.copyOf(Files.readLines(moduleFactoryFile, StandardCharsets.UTF_8));
        Set<String> expectedLines = ImmutableSet.of(
                EXPECTED_PACKAGE_PREFIX + ".threads.java.EventBusModuleFactory",
                EXPECTED_PACKAGE_PREFIX + ".threads.java.AsyncEventBusModuleFactory",
                EXPECTED_PACKAGE_PREFIX + ".threads.java.DynamicThreadPoolModuleFactory",
                EXPECTED_PACKAGE_PREFIX + ".threads.java.NamingThreadFactoryModuleFactory",
                EXPECTED_PACKAGE_PREFIX + ".threads.java.ThreadPoolRegistryImplModuleFactory");

        assertEquals(expectedLines, lines);
    }

    private static void verifyXmlFiles(final Collection<File> xmlFiles) throws Exception {
        ErrorHandler errorHandler = new ErrorHandler() {

            @Override
            public void warning(final SAXParseException exception)
                    throws SAXException {
                fail("Generated blueprint xml is not well formed " + exception.getMessage());
            }

            @Override
            public void fatalError(final SAXParseException exception)
                    throws SAXException {
                fail("Generated blueprint xml is not well formed " + exception.getMessage());
            }

            @Override
            public void error(final SAXParseException exception) throws SAXException {
                fail("Generated blueprint xml is not well formed "  + exception.getMessage());
            }
        };

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        for (File file : xmlFiles) {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            builder.parse(new InputSource(file.getPath()));
        }
    }

    private static void assertAbstractNamingThreadFactoryModuleFactory( final MbeASTVisitor visitor) {
        assertContains(visitor.implmts, "org.opendaylight.controller.config.spi.ModuleFactory");

        visitor.assertFields(2);
        visitor.assertField("public static final java.lang.String NAME = \"threadfactory-naming\"");
        visitor.assertField(
                "private static final java.util.Set<Class<? extends org.opendaylight.controller.config.api.annotations.AbstractServiceInterface>> serviceIfcs");

        assertFactoryMethods(visitor.methods, 9);
        visitor.assertMethodDescriptions(0);
        visitor.assertMethodJavadocs(0);
    }

    private static void assertFactoryMethods(final Set<String> methods, final int expectedSize) {

        List<ArgumentAssertion> args = new ArrayList<>();
        ArgumentAssertion oldInstanceArg = new ArgumentAssertion(DynamicMBeanWithInstance.class.getCanonicalName(), "old");
        ArgumentAssertion instanceNameArg = new ArgumentAssertion(String.class.getSimpleName(), "instanceName");
        ArgumentAssertion dependencyResolverArg = new ArgumentAssertion(DependencyResolver.class.getCanonicalName(), "dependencyResolver");
        ArgumentAssertion bundleContextArg = new ArgumentAssertion(BundleContext.class.getCanonicalName(), "bundleContext");

        assertMethodPresent(methods, new MethodAssertion(String.class.getSimpleName(), "getImplementationName"));

        args.add(instanceNameArg);
        args.add(dependencyResolverArg);
        args.add(bundleContextArg);
        assertMethodPresent(methods, new MethodAssertion(Module.class.getCanonicalName(), "createModule", args));

        args.add(2, oldInstanceArg);
        assertMethodPresent(methods, new MethodAssertion(Module.class.getCanonicalName(), "createModule", args));

        args.clear();
        args.add(oldInstanceArg);
        assertMethodPresent(methods, new MethodAssertion("org.opendaylight.controller.config.threads.java.NamingThreadFactoryModule", "handleChangedClass", args));

        args.clear();
        args.add(instanceNameArg);
        args.add(dependencyResolverArg);
        args.add(bundleContextArg);
        assertMethodPresent(methods, new MethodAssertion("org.opendaylight.controller.config.threads.java.NamingThreadFactoryModule", "instantiateModule", args));


        args.add(2, new ArgumentAssertion("org.opendaylight.controller.config.threads.java.NamingThreadFactoryModule", "oldModule"));
        args.add(3, new ArgumentAssertion(AutoCloseable.class.getCanonicalName(), "oldInstance"));
        assertMethodPresent(methods, new MethodAssertion("org.opendaylight.controller.config.threads.java.NamingThreadFactoryModule", "instantiateModule", args));

        args.clear();
        args.add(new ArgumentAssertion(DependencyResolverFactory.class.getCanonicalName(), "dependencyResolverFactory"));
        args.add(bundleContextArg);
        assertMethodPresent(methods, new MethodAssertion("java.util.Set<org.opendaylight.controller.config.threads.java.NamingThreadFactoryModule>", "getDefaultModules", args));

        args.clear();
        args.add(new ArgumentAssertion("Class<? extends org.opendaylight.controller.config.api.annotations.AbstractServiceInterface>", "serviceInterface"));
        assertMethodPresent(methods, new MethodAssertion("boolean", "isModuleImplementingServiceInterface", args));

        assertEquals(methods.size(), expectedSize);
    }

    private static void assertMethodPresent(final Set<String> methods, final MethodAssertion methodAssertion) {
        assertTrue(String.format("Generated methods did not contain %s, generated methods: %s",
                methodAssertion.toString(), methods), methods.contains(methodAssertion.toString()));
    }

    private static void assertAbstractDynamicThreadPoolModule(final MbeASTVisitor visitor) {
        assertNotNull(visitor.javadoc);
        assertContains(visitor.descriptionAnotValue, "threadpool-dynamic description");
        assertContains(visitor.implmts,
                EXPECTED_PACKAGE_PREFIX + ".threads.java.DynamicThreadPoolModuleMXBean",
                EXPECTED_PACKAGE_PREFIX + ".threads.ScheduledThreadPoolServiceInterface",
                EXPECTED_PACKAGE_PREFIX + ".threads.ThreadPoolServiceInterface");
        assertContains(visitor.extnds, AbstractModule.class.getCanonicalName());
        visitor.assertConstructors(2);
        visitor.assertFields(17);
        visitor.assertField("private java.lang.Long maximumSize");
        visitor.assertField("private javax.management.ObjectName threadfactory");
        visitor.assertField("private java.util.concurrent.ThreadFactory threadfactoryDependency");
        visitor.assertField("private java.lang.Long keepAlive = new java.lang.Long(\"10\")");
        visitor.assertField("private java.lang.Long coreSize");
        visitor.assertField("private byte[] binary");

        assertEquals(1, visitor.requireIfc.size());
        String reqIfc = visitor.requireIfc.get("setThreadfactory");
        assertNotNull("Missing generated setter for threadfactory", reqIfc);
        assertContains(reqIfc, EXPECTED_PACKAGE_PREFIX + ".threads.ThreadFactoryServiceInterface");

        assertEquals("Incorrenct number of generated methods", 26, visitor.methods.size());
        visitor.assertMethodDescriptions(3);
        visitor.assertMethodJavadocs(3);
        visitor.assertMethodJavadoc("setMaximumSize", "void setMaximumSize(java.lang.Long maximumSize)");

    }

    private static void assertContains(final String source, final String... contained) {
        for (String string : contained) {
            assertThat(source, containsString(string));
        }
    }

    private static void verifyFile(final File file, final AbstractVerifier verifier) throws ParseException, IOException {
        final CompilationUnit cu = JavaParser.parse(file);
        cu.accept(verifier, null);
        verifier.verify();
    }

    private static class MethodAssertion extends ArgumentAssertion {

        private final List<ArgumentAssertion> arguments;


        MethodAssertion(final String type, final String name, final List<ArgumentAssertion> arguments) {
            super(type, name);
            this.arguments = arguments;
        }

        MethodAssertion(final String type, final String name) {
            this(type, name, Collections.<ArgumentAssertion>emptyList());
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(type).append(' ');
            sb.append(name).append('(');

            int i = 0;
            for (ArgumentAssertion argument : arguments) {
                sb.append(argument.type).append(' ');
                sb.append(argument.name);
                if(++i != arguments.size()) {
                    sb.append(", ");
                }
            }
            sb.append(')');
            return sb.toString();
        }
    }

    private static class ArgumentAssertion {

        protected final String type, name;

        private ArgumentAssertion(final String type, final String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(type).append(' ');
            sb.append(name);
            return sb.toString();
        }
    }
}
