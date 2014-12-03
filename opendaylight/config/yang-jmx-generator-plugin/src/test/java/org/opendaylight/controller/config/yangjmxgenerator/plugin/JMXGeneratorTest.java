/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.spi.AbstractModule;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslatorTest;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntryTest;
import org.osgi.framework.BundleContext;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

//TODO: refactor
public class JMXGeneratorTest extends AbstractGeneratorTest {

    JMXGenerator jmxGenerator;

    protected final HashMap<String, String> map = Maps.newHashMap();
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
                ConfigConstants.CONFIG_NAMESPACE
                        + JMXGenerator.NAMESPACE_TO_PACKAGE_DIVIDER
                        + PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX);
        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "false");
        jmxGenerator = new JMXGenerator(new CodeWriter());
        jmxGenerator.setAdditionalConfig(map);
        File targetDir = new File(generatorOutputPath, "target");
        generatedResourcesDir = new File(targetDir, "generated-resources");
        jmxGenerator.setResourceBaseDir(generatedResourcesDir);
        Log mockedLog = mock(Log.class);
        doReturn(false).when(mockedLog).isDebugEnabled();
        doNothing().when(mockedLog).debug(any(CharSequence.class));
        doNothing().when(mockedLog).info(any(CharSequence.class));
        doNothing().when(mockedLog).error(any(CharSequence.class),
                any(Throwable.class));
        jmxGenerator.setLog(mockedLog);
        MavenProject project = mock(MavenProject.class);
        doReturn(generatorOutputPath).when(project).getBasedir();
        jmxGenerator.setMavenProject(project);
        outputBaseDir = JMXGenerator.concatFolders(targetDir,
                "generated-sources", "config");
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

    private void verifyModuleFactoryFile(boolean shouldBePresent) {
        File factoryFile = new File(generatedResourcesDir, "META-INF"
                + File.separator + "services" + File.separator
                + ModuleFactory.class.getName());
        if (!shouldBePresent)
            assertFalse("Factory file should not be generated",
                    factoryFile.exists());
        else
            assertTrue("Factory file should be generated", factoryFile.exists());
    }

    public static List<String> toFileNames(Collection<File> files) {
        List<String> result = new ArrayList<>();
        for (File f : files) {
            result.add(f.getName());
        }
        Collections.sort(result);
        return result;
    }

    @Test
    public void generateSIEsTest() throws IOException {
        Collection<File> files = jmxGenerator.generateSources(context,
                outputBaseDir, Sets.newHashSet(threadsModule));
        assertEquals(ServiceInterfaceEntryTest.expectedSIEFileNames,
                toFileNames(files));

        Map<String, ASTVisitor> verifiers = Maps.newHashMap();

        for (File file : files) {
            verifiers.put(file.getName(), new SieASTVisitor());
        }

        processGeneratedCode(files, verifiers);

        for (File file : files) {
            String fileName = file.getName();
            SieASTVisitor verifier = (SieASTVisitor) verifiers.get(fileName);

            assertEquals(fileName.substring(0, fileName.length() - 5),
                    verifier.type);
            assertThat(
                    verifier.extnds,
                    containsString("org.opendaylight.controller.config.api.annotations.AbstractServiceInterface"));
            assertEquals(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                    + ".threads", verifier.packageName);
            assertNotNull(verifier.javadoc);

            if ("ThreadPoolServiceInterface.java".equals(fileName)) {
                assertContains(verifier.descriptionAnotValue,
                        "A simple pool of threads able to execute work.");
                assertContains(verifier.sieAnnotValue, "threadpool");
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                                + ".threadpool.ThreadPool.class");
            } else if ("ScheduledThreadPoolServiceInterface.java"
                    .equals(fileName)) {
                assertContains(verifier.extnds,
                        PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                                + ".threads.ThreadPoolServiceInterface");
                assertContains(
                        verifier.descriptionAnotValue,
                        "An extension of the simple pool of threads able to schedule work to be executed at some point in time.");
                assertContains(verifier.sieAnnotValue, "scheduled-threadpool");
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                                + ".threadpool.ScheduledThreadPool.class");
            } else if ("EventBusServiceInterface.java".equals(fileName)) {
                assertContains(
                        verifier.descriptionAnotValue,
                        "Service representing an event bus. The service acts as message router between event producers and event consumers");
                assertContains(verifier.sieAnnotValue, "eventbus");
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        "com.google.common.eventbus.EventBus.class");
            } else if ("ThreadFactoryServiceInterface.java".equals(fileName)) {
                assertContains(
                        verifier.descriptionAnotValue,
                        "Service representing a ThreadFactory instance. It is directly useful in Java world, where various library pieces need to create threads and you may want to inject a customized thread implementation.");
                assertContains(verifier.sieAnnotValue, "threadfactory");
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        "java.util.concurrent.ThreadFactory.class");

            } else if ("ScheduledExecutorServiceServiceInterface.java"
                    .equals(fileName)) {
                assertContains(verifier.sieAnnotOsgiRegistrationType,
                        "java.util.concurrent.ScheduledExecutorService.class");
            } else {
                fail("Unknown generated sie " + fileName);
            }
        }
    }

    @Test
    public void generateMBEsListTest() throws Exception {
        // default value for module factory file is true
        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "randomValue");
        jmxGenerator.setAdditionalConfig(map);

        Collection<File> files = jmxGenerator.generateSources(context,
                outputBaseDir, Sets.newHashSet(bgpListenerJavaModule));

        assertEquals(expectedGenerateMBEsListNames, toFileNames(files));
    }

    @Test
    public void generateMBEsTest() throws Exception {
        // default value for module factory file is true
        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "randomValue");
        jmxGenerator.setAdditionalConfig(map);

        Collection<File> files = jmxGenerator.generateSources(context,
                outputBaseDir, Sets.newHashSet(threadsJavaModule));

        assertEquals(expectedModuleFileNames, toFileNames(files));

        Map<String, ASTVisitor> verifiers = Maps.newHashMap();

        Collection<File> xmlFiles = Collections2.filter(files,
                new Predicate<File>() {

                    @Override
                    public boolean apply(File input) {
                        return input.getName().endsWith("xml");
                    }
                });

        Collection<File> javaFiles = Collections2.filter(files,
                new Predicate<File>() {

                    @Override
                    public boolean apply(File input) {
                        return input.getName().endsWith("java");
                    }
                });

        MbeASTVisitor abstractDynamicThreadPoolModuleVisitor = null;
        MbeASTVisitor asyncEventBusModuleMXBeanVisitor = null;
        MbeASTVisitor abstractNamingThreadFactoryModuleFactoryVisitor = null;
        MbeASTVisitor asyncEventBusModuleVisitor = null;
        MbeASTVisitor eventBusModuleFactoryVisitor = null;

        for (File file : javaFiles) {
            String name = file.getName();
            MbeASTVisitor visitor = new MbeASTVisitor();
            verifiers.put(name, visitor);
            if (name.equals("AbstractDynamicThreadPoolModule.java"))
                abstractDynamicThreadPoolModuleVisitor = visitor;
            if (name.equals("AsyncEventBusModuleMXBean.java"))
                asyncEventBusModuleMXBeanVisitor = visitor;
            if (name.equals("AbstractNamingThreadFactoryModuleFactory.java"))
                abstractNamingThreadFactoryModuleFactoryVisitor = visitor;
            if (name.equals("AsyncEventBusModule.java"))
                asyncEventBusModuleVisitor = visitor;
            if (name.equals("EventBusModuleFactory.java"))
                eventBusModuleFactoryVisitor = visitor;
        }

        processGeneratedCode(javaFiles, verifiers);

        assertAbstractDynamicThreadPoolModule(abstractDynamicThreadPoolModuleVisitor);
        assertAsyncEventBusModuleMXBean(asyncEventBusModuleMXBeanVisitor);
        assertAbstractNamingThreadFactoryModuleFactory(abstractNamingThreadFactoryModuleFactoryVisitor);
        assertAsyncEventBusModule(asyncEventBusModuleVisitor);
        assertEventBusModuleFactory(eventBusModuleFactoryVisitor);

        verifyXmlFiles(xmlFiles);
        // verify ModuleFactory file
        File moduleFactoryFile = JMXGenerator.concatFolders(
                generatedResourcesDir, "META-INF", "services",
                ModuleFactory.class.getName());
        assertThat(moduleFactoryFile.exists(), is(true));
        Set<String> lines = Sets.newHashSet(FileUtils
                .readLines(moduleFactoryFile));
        Set<String> expectedLines = Sets.newHashSet(//
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.EventBusModuleFactory",//
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.AsyncEventBusModuleFactory", //
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.DynamicThreadPoolModuleFactory",//
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.NamingThreadFactoryModuleFactory", //
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.ThreadPoolRegistryImplModuleFactory");


        assertThat(lines, equalTo(expectedLines));

    }

    private void verifyXmlFiles(Collection<File> xmlFiles) throws Exception {
        ErrorHandler errorHandler = new ErrorHandler() {

            @Override
            public void warning(SAXParseException exception)
                    throws SAXException {
                fail("Generated blueprint xml is not well formed "
                        + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception)
                    throws SAXException {
                fail("Generated blueprint xml is not well formed "
                        + exception.getMessage());
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                fail("Generated blueprint xml is not well formed "
                        + exception.getMessage());
            }
        };

        for (File file : xmlFiles) {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            builder.setErrorHandler(errorHandler);
            builder.parse(new InputSource(file.getPath()));
        }

    }

    private void assertEventBusModuleFactory(MbeASTVisitor visitor) {
        assertEquals(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                + ".threads.java", visitor.packageName);
        assertEquals("EventBusModuleFactory", visitor.type);
        assertContains(visitor.extnds,
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.AbstractEventBusModuleFactory");

        assertEquals(0, visitor.fieldDeclarations.size());

        assertEquals("Incorrenct number of generated methods", 0,
                visitor.methods.size());
        assertEquals("Incorrenct number of generated constructors", 0,
                visitor.constructors.size());
        assertEquals("Incorrenct number of generated method descriptions", 0,
                visitor.methodDescriptions.size());
        assertEquals("Incorrenct number of generated method javadoc", 0,
                visitor.methodJavadoc.size());
    }

    private void assertAsyncEventBusModule(MbeASTVisitor visitor) {
        assertEquals(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                + ".threads.java", visitor.packageName);
        assertEquals("AsyncEventBusModule", visitor.type);
        assertContains(visitor.extnds,
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.AbstractAsyncEventBusModule");

        assertEquals(0, visitor.fieldDeclarations.size());

        assertEquals("Incorrenct number of generated methods", 2,
                visitor.methods.size());
        assertEquals("Incorrenct number of generated constructors", 2,
                visitor.constructors.size());
        assertEquals("Incorrenct number of generated method descriptions", 0,
                visitor.methodDescriptions.size());
        assertEquals("Incorrenct number of generated method javadoc", 0,
                visitor.methodJavadoc.size());
    }

    private void assertAbstractNamingThreadFactoryModuleFactory(
            MbeASTVisitor visitor) {
        assertEquals(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                + ".threads.java", visitor.packageName);
        assertEquals("AbstractNamingThreadFactoryModuleFactory", visitor.type);
        assertContains(visitor.implmts,
                "org.opendaylight.controller.config.spi.ModuleFactory");
        Set<String> fieldDeclarations = visitor.fieldDeclarations;
        assertDeclaredField(fieldDeclarations,
                "public static final java.lang.String NAME=\"threadfactory-naming\"");
        assertDeclaredField(
                fieldDeclarations,
                "private static final java.util.Set<Class<? extends org.opendaylight.controller.config.api.annotations.AbstractServiceInterface>> serviceIfcs");

        assertEquals(2, fieldDeclarations.size());

        assertFactoryMethods(visitor.methods, 9);
        assertEquals("Incorrenct number of generated method descriptions", 0,
                visitor.methodDescriptions.size());
        assertEquals("Incorrenct number of generated method javadoc", 0,
                visitor.methodJavadoc.size());

    }

    private void assertFactoryMethods(Set<String> methods, int expectedSize) {

        List<ArgumentAssertion> args = Lists.newArrayList();
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

    private void assertMethodPresent(Set<String> methods, MethodAssertion methodAssertion) {
        assertTrue(String.format("Generated methods did not contain %s, generated methods: %s",
                methodAssertion.toString(), methods), methods.contains(methodAssertion.toString()));
    }

    private void assertAsyncEventBusModuleMXBean(MbeASTVisitor visitor) {
        assertEquals(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                + ".threads.java", visitor.packageName);
        assertEquals("AsyncEventBusModuleMXBean", visitor.type);

        assertEquals("Incorrenct number of generated methods", 4,
                visitor.methods.size());

    }

    private void assertAbstractDynamicThreadPoolModule(MbeASTVisitor visitor) {
        assertEquals(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                + ".threads.java", visitor.packageName);
        assertNotNull(visitor.javadoc);
        assertContains(visitor.descriptionAnotValue,
                "threadpool-dynamic description");
        assertEquals("AbstractDynamicThreadPoolModule", visitor.type);
        assertContains(visitor.implmts,
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.java.DynamicThreadPoolModuleMXBean",
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.ScheduledThreadPoolServiceInterface",
                PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threads.ThreadPoolServiceInterface");
        assertContains(visitor.extnds, AbstractModule.class.getCanonicalName());
        assertEquals(2, visitor.constructors.size());
        Set<String> fieldDeclarations = visitor.fieldDeclarations;
        assertDeclaredField(fieldDeclarations,
                "private java.lang.Long maximumSize");
        assertDeclaredField(fieldDeclarations,
                "private javax.management.ObjectName threadfactory");
        assertDeclaredField(fieldDeclarations,
                "private java.util.concurrent.ThreadFactory threadfactoryDependency");
        assertDeclaredField(fieldDeclarations,
                "private java.lang.Long keepAlive=new java.lang.Long(\"10\")");
        assertDeclaredField(fieldDeclarations,
                "private java.lang.Long coreSize");
        assertDeclaredField(fieldDeclarations, "private byte[] binary");
        assertEquals(17, fieldDeclarations.size());

        assertEquals(1, visitor.requireIfc.size());
        String reqIfc = visitor.requireIfc.get("setThreadfactory");
        assertNotNull("Missing generated setter for threadfactory", reqIfc);
        assertContains(reqIfc, PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                + ".threads.ThreadFactoryServiceInterface");

        assertEquals("Incorrenct number of generated methods", 26,
                visitor.methods.size());
        assertEquals("Incorrenct number of generated method descriptions", 3,
                visitor.methodDescriptions.size());
        assertEquals("Incorrenct number of generated method javadoc", 3,
                visitor.methodJavadoc.size());
        assertNotNull("Missing javadoc for setMaximumSize method " + visitor.methodJavadoc,
                visitor.methodJavadoc.get("void setMaximumSize(java.lang.Long maximumSize)"));
    }

    private void assertDeclaredField(Set<String> fieldDeclarations,
            String declaration) {
        assertTrue("Missing field " + declaration + ", got: "
                + fieldDeclarations,
                fieldDeclarations.contains(declaration + ";\n"));
    }

    private static class SieASTVisitor extends ASTVisitor {
        protected String packageName, descriptionAnotValue, sieAnnotValue,
        sieAnnotOsgiRegistrationType, type, extnds, javadoc;
        protected Map<String, String> methodDescriptions = Maps.newHashMap();

        @Override
        public boolean visit(PackageDeclaration node) {
            packageName = node.getName().toString();
            return super.visit(node);
        }

        @Override
        public boolean visit(NormalAnnotation node) {
            if (node.getTypeName().toString()
                    .equals(Description.class.getCanonicalName())) {
                if (node.getParent() instanceof TypeDeclaration) {
                    descriptionAnotValue = node.values().get(0).toString();
                } else if (node.getParent() instanceof MethodDeclaration) {
                    String descr = node.values().get(0).toString();
                    methodDescriptions.put(((MethodDeclaration) node
                            .getParent()).getName().toString(), descr);
                }
            } else if (node
                    .getTypeName()
                    .toString()
                    .equals(ServiceInterfaceAnnotation.class.getCanonicalName())) {
                String text1 = node.values().get(0).toString();
                String text2 = node.values().get(1).toString();
                if (text1.contains("value")) {
                    sieAnnotValue = text1;
                    sieAnnotOsgiRegistrationType = text2;
                } else {
                    sieAnnotValue = text2;
                    sieAnnotOsgiRegistrationType = text1;
                }
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            javadoc = node.getJavadoc() == null ? null : node.getJavadoc()
                    .toString();
            type = node.getName().toString();
            List<?> superIfcs = node.superInterfaceTypes();
            extnds = superIfcs != null && !superIfcs.isEmpty() ? superIfcs
                    .toString() : null;
            return super.visit(node);
        }
    }

    private static class MbeASTVisitor extends SieASTVisitor {
        private String implmts;
        private final Set<String> fieldDeclarations = Sets.newHashSet();
        private final Set<String> constructors = Sets.newHashSet();
        private final Set<String> methods = new HashSet<String>();
        private final Map<String, String> requireIfc = Maps.newHashMap();
        private final Map<String, String> methodJavadoc = Maps.newHashMap();

        @Override
        public boolean visit(NormalAnnotation node) {
            boolean result = super.visit(node);
            if (node.getTypeName().toString()
                    .equals(RequireInterface.class.getCanonicalName())
                    && node.getParent() instanceof MethodDeclaration) {
                // remember only top level description annotation
                String reqVal = node.values().get(0).toString();
                requireIfc.put(((MethodDeclaration) node.getParent()).getName()
                        .toString(), reqVal);
            }
            return result;
        }

        @Override
        public boolean visit(FieldDeclaration node) {
            fieldDeclarations.add(node.toString());
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if (node.isConstructor())
                constructors.add(node.toString());
            else {
                String methodSignature = node.getReturnType2() + " " + node.getName() + "(";
                boolean first = true;
                for (Object o : node.parameters()) {
                    if (first){
                        first = false;
                    } else {
                        methodSignature += ",";
                    }
                    methodSignature += o.toString();
                }
                methodSignature += ")";
                methods.add(methodSignature);
                if (node.getJavadoc() != null) {
                    methodJavadoc.put(methodSignature, node.getJavadoc().toString());
                }
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            boolean visit = super.visit(node);
            List<?> superIfcs = node.superInterfaceTypes();
            implmts = superIfcs != null && !superIfcs.isEmpty() ? superIfcs
                    .toString() : null;
            extnds = node.getSuperclassType() == null ? null : node
                    .getSuperclassType().toString();
            return visit;
        }

    }

    private void assertContains(String source, String... contained) {
        for (String string : contained) {
            assertThat(source, containsString(string));
        }
    }

    private void processGeneratedCode(Collection<File> files,
            Map<String, ASTVisitor> verifiers) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
        parser.setCompilerOptions(options);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        for (File file : files) {
            char[] source = readFileAsChars(file);
            parser.setSource(source);
            parser.setResolveBindings(true);

            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            // Check for compilation problems in generated file
            for (IProblem c : cu.getProblems()) {
                // 1610613332 = Syntax error, annotations are only available if
                // source level is 5.0
                if (c.getID() == 1610613332)
                    continue;
                // 1610613332 = Syntax error, parameterized types are only
                // available if source level is 5.0
                if (c.getID() == 1610613329)
                    continue;
                if (c.getID() == 1610613328) // 'for each' statements are only available if source level is 5.0
                    continue;
                fail("Error in generated source code " + file + ":"
                        + c.getSourceLineNumber() + " id: " + c.getID() + " message:"  + c.toString());
            }

            ASTVisitor visitor = verifiers.get(file.getName());
            if (visitor == null)
                fail("Unknown generated file " + file.getName());
            cu.accept(visitor);

        }
    }

    public static char[] readFileAsChars(File file) throws IOException {
        List<String> readLines = Files
                .readLines(file, Charset.forName("utf-8"));
        char[] retVal = new char[0];
        for (String string : readLines) {
            char[] line = string.toCharArray();
            int beforeLength = retVal.length;
            retVal = Arrays.copyOf(retVal, retVal.length + line.length + 1);
            System.arraycopy(line, 0, retVal, beforeLength, line.length);
            retVal[retVal.length - 1] = '\n';
        }
        return retVal;
    }

    private static class MethodAssertion extends ArgumentAssertion{

        private List<ArgumentAssertion> arguments;


        MethodAssertion(String type, String name, List<ArgumentAssertion> arguments) {
            super(type, name);
            this.arguments = arguments;
        }

        MethodAssertion(String type, String name) {
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
                if(++i != arguments.size())
                    sb.append(',');
            }
            sb.append(')');
            return sb.toString();
        }
    }

    private static class ArgumentAssertion {

        protected final String type, name;

        private ArgumentAssertion(String type, String name) {
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
