/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.it;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import javax.inject.Inject;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.config.yang.config.toaster_consumer.impl.ToasterConsumerRuntimeMXBean;
import org.opendaylight.controller.config.yang.config.toaster_provider.impl.ToasterProviderRuntimeMXBean;
import org.opendaylight.controller.sample.toaster.provider.api.ToastConsumer;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.HashBrown;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WhiteBread;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;

import java.lang.management.ManagementFactory;

@RunWith(PaxExam.class)
public class ToasterTest {

    @Inject
    @Filter(timeout=60*1000)
    ToastConsumer toastConsumer;

    @Configuration
    public Option[] config() {
        return options(systemProperty("osgi.console").value("2401"), mavenBundle("org.slf4j", "slf4j-api")
                .versionAsInProject(), //
                          mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //

                                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                                + "/src/test/resources/logback.xml"),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),

                toasterBundles(),
                mdSalCoreBundles(),

                bindingAwareSalBundles(),
                configMinumumBundles(),
                // BASE Models
                baseModelBundles(),
                flowCapableModelBundles(),

                // Set fail if unresolved bundle present
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                junitAndMockitoBundles());
    }

    private Option toasterBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.opendaylight.controller.samples", "sample-toaster-provider").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.samples", "sample-toaster-consumer").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.samples", "sample-toaster").versionAsInProject()
        );
    }

    @Test
    public void testToaster() throws Exception {

        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName consumerOn = new ObjectName("org.opendaylight.controller:instanceName=toaster-consumer-impl,type=RuntimeBean,moduleFactoryName=toaster-consumer-impl");
        ObjectName providerOn = new ObjectName("org.opendaylight.controller:instanceName=toaster-provider-impl,type=RuntimeBean,moduleFactoryName=toaster-provider-impl");

        long toastsMade = (long) platformMBeanServer.getAttribute(providerOn, "ToastsMade");
        assertEquals(0, toastsMade);

        boolean toasts = true;

        // Make toasts using OSGi service
        toasts &= toastConsumer.createToast(HashBrown.class, 4);
        toasts &= toastConsumer.createToast(WhiteBread.class, 8);

        // Make toast using JMX/config-subsystem
        toasts &= (Boolean)platformMBeanServer.invoke(consumerOn, "makeHashBrownToast", new Object[]{4}, new String[]{Integer.class.getName()});

        Assert.assertTrue("Not all toasts done by " + toastConsumer, toasts);

        // Verify toasts made count on provider via JMX/config-subsystem
        toastsMade = (long) platformMBeanServer.getAttribute(providerOn, "ToastsMade");
        assertEquals(3, toastsMade);
    }

}
