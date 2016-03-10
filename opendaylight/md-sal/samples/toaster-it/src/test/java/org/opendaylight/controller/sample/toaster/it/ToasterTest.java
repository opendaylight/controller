/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import java.lang.management.ManagementFactory;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sample.kitchen.api.EggsType;
import org.opendaylight.controller.sample.kitchen.api.KitchenService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.HashBrown;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WhiteBread;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.util.Filter;

@RunWith(PaxExam.class)
public class ToasterTest extends AbstractMdsalTestBase {
    @Inject
    @Filter(timeout=60*1000)
    KitchenService kitchenService;

    @Override
    public String getModuleName() {
        return "toaster-provider-impl";
    }

    @Override
    public String getInstanceName() {
        return "toaster-provider-impl";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven().groupId("org.opendaylight.controller").artifactId("features-mdsal").classifier("features")
                .type("xml").versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-toaster";
    }

    @Test
    public void testToaster() throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName providerOn = new ObjectName("org.opendaylight.controller:instanceName=toaster-provider-impl,type=RuntimeBean,moduleFactoryName=toaster-provider-impl");

        long toastsMade = (long) platformMBeanServer.getAttribute(providerOn, "ToastsMade");
        assertEquals(0, toastsMade);

        boolean success = true;

        // Make toasts using OSGi service
        success &= kitchenService.makeBreakfast( EggsType.SCRAMBLED, HashBrown.class, 4).get().isSuccessful();
        success &= kitchenService.makeBreakfast( EggsType.POACHED, WhiteBread.class, 8 ).get().isSuccessful();

        assertTrue("Not all breakfasts succeeded", success);

        // Verify toasts made count on provider via JMX/config-subsystem
        toastsMade = (long) platformMBeanServer.getAttribute(providerOn, "ToastsMade");
        assertEquals(2, toastsMade);
    }
}
