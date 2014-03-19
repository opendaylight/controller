/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it.pax;

import static org.junit.Assert.fail;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.baseModelBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.bindingAwareSalBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.configMinumumBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.flowCapableModelBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.junitAndMockitoBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.mdSalCoreBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.RunWith;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.test.TestingNetconfClient;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

@Ignore
@RunWith(PaxExam.class)
public class IdentityRefNetconfTest {

    public static final int CLIENT_CONNECTION_TIMEOUT_MILLIS = 15000;

    // Wait for controller to start
    @Inject
    @Filter(timeout = 60 * 1000)
    BindingAwareBroker broker;

    @Configuration
    public Option[] config() {
        return options(
                systemProperty("osgi.console").value("2401"),
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                systemPackages("sun.nio.ch"),

                testingModules(),
                loggingModules(),
                mdSalCoreBundles(),
                bindingAwareSalBundles(), configMinumumBundles(), baseModelBundles(), flowCapableModelBundles(),
                junitAndMockitoBundles(),

                // Classes from test-jars bundled for pax-exam test
                streamBundle(TinyBundles.bundle()
                        .add(TestingNetconfClient.class)
                        .add(XmlFileLoader.class)

                        .add("/netconfMessages/editConfig_identities.xml",
                                XmlFileLoader.class.getResource("/netconfMessages/editConfig_identities.xml"))
                        .add("/netconfMessages/commit.xml",
                                XmlFileLoader.class.getResource("/netconfMessages/commit.xml"))
                        .add("/netconfMessages/getConfig.xml",
                                XmlFileLoader.class.getResource("/netconfMessages/getConfig.xml"))

                        .set(Constants.BUNDLE_SYMBOLICNAME, "TestingClient_bundle")
                        .set(Constants.EXPORT_PACKAGE, "org.opendaylight.controller.netconf.client.test, " +
                                "org.opendaylight.controller.netconf.util.test")
                        .build(TinyBundles.withBnd())));
    }

    private Option loggingModules() {
        return new DefaultCompositeOption(
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.thirdparty", "nagasena").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.thirdparty", "nagasena-rta").versionAsInProject());


    }

    private Option testingModules() {
        return new DefaultCompositeOption(
                mavenBundle("org.opendaylight.controller", "yang-test").versionAsInProject());
    }

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 18383);

    @Test
    public void testIdRef() throws Exception {
        Preconditions.checkNotNull(broker, "Controller not initialized");

        NioEventLoopGroup nettyThreadgroup = new NioEventLoopGroup();
        NetconfClientDispatcher clientDispatcher = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup,
                CLIENT_CONNECTION_TIMEOUT_MILLIS);

        NetconfMessage edit = xmlFileToNetconfMessage("netconfMessages/editConfig_identities.xml");
        NetconfMessage commit = xmlFileToNetconfMessage("netconfMessages/commit.xml");
        NetconfMessage getConfig = xmlFileToNetconfMessage("netconfMessages/getConfig.xml");

        try (TestingNetconfClient netconfClient = new TestingNetconfClient("client", tcpAddress, CLIENT_CONNECTION_TIMEOUT_MILLIS, clientDispatcher)) {
            sendMessage(edit, netconfClient);
            sendMessage(commit, netconfClient);
            sendMessage(getConfig, netconfClient, "id-test",
                        "<prefix:afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</prefix:afi>",
                        "<prefix:afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</prefix:afi>",
                        "<prefix:safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</prefix:safi>",
                        "<prefix:safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</prefix:safi>");

            clientDispatcher.close();
        } catch (Exception e) {
            fail(Throwables.getStackTraceAsString(e));
        }
    }

    private void sendMessage(NetconfMessage edit, TestingNetconfClient netconfClient, String... containingResponse)
            throws ExecutionException, InterruptedException, TimeoutException {
        NetconfMessage response = netconfClient.sendRequest(edit).get();
        if (containingResponse == null) {
            Assert.assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString("<ok/>"));
        } else {
            for (String resp : containingResponse) {
                Assert.assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString(resp));
            }
        }
    }

    public static NetconfMessage xmlFileToNetconfMessage(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        return XmlFileLoader.xmlFileToNetconfMessage(fileName);
    }
}
