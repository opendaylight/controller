/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it.pax;

import static org.opendaylight.controller.test.sal.binding.it.TestHelper.baseModelBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.bindingAwareSalBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.configMinumumBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.flowCapableModelBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.junitAndMockitoBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.mdSalCoreBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.base.Preconditions;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.RunWith;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RunWith(PaxExam.class)
public class IdentityRefNetconfTest {

    public static final int CLIENT_CONNECTION_TIMEOUT_MILLIS = 5000;

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

                testingModules(),
                loggingModules(),
                mdSalCoreBundles(),
                bindingAwareSalBundles(), configMinumumBundles(), baseModelBundles(), flowCapableModelBundles(),
                junitAndMockitoBundles());
    }

    private Option loggingModules() {
        return new DefaultCompositeOption(
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject());
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

        try (NetconfClient netconfClient = new NetconfClient("client", tcpAddress, CLIENT_CONNECTION_TIMEOUT_MILLIS, clientDispatcher)) {
            sendMessage(edit, netconfClient);
            sendMessage(commit, netconfClient);
            sendMessage(getConfig, netconfClient, "id-test",
                    "<afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</afi>",
                    "<afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</afi>",
                    "<safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</safi>",
                    "<safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</safi>");
        }

        clientDispatcher.close();
    }

    private void sendMessage(NetconfMessage edit, NetconfClient netconfClient, String... containingResponse)
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
        return new NetconfMessage(xmlFileToDocument(fileName));
    }

    public static Document xmlFileToDocument(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        // TODO xml messages from netconf-util test-jar cannot be loaded here(in OSGi), since test jar is not a bundle
        try (InputStream resourceAsStream = IdentityRefNetconfTest.class.getClassLoader().getResourceAsStream(fileName)) {
            Preconditions.checkNotNull(resourceAsStream);
            final Document doc = XmlUtil.readXmlToDocument(resourceAsStream);
            return doc;
        }
    }
}
