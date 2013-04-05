/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin.it;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

public class YangToResourcesPluginTest {

    @Test
    public void testCorrect() throws VerificationException {
        Verifier v = YangToSourcesPluginTest.setUp("Correct_resources/", false);
        verifyCorrectLog(v);
    }

    static void verifyCorrectLog(Verifier v) throws VerificationException {
        v.verifyErrorFreeLog();
        v.verifyTextInLog("[INFO] yang-to-resources: Resource provider instantiated from org.opendaylight.controller.yang2sources.spi.ResourceProviderTestImpl");
        v.verifyTextInLog("[INFO] yang-to-resources: Resource provider org.opendaylight.controller.yang2sources.spi.ResourceProviderTestImpl call successful");
    }

    @Test
    public void testNoGenerators() throws VerificationException {
        Verifier v = YangToSourcesPluginTest.setUp("NoGenerators_resources/",
                false);
        v.verifyErrorFreeLog();
        v.verifyTextInLog("[WARNING] yang-to-resources: No resource provider classes provided");
    }

    @Test
    public void testUnknownGenerator() throws VerificationException {
        Verifier v = YangToSourcesPluginTest.setUp(
                "UnknownGenerator_resources/", true);
        v.verifyTextInLog("[ERROR] yang-to-resources: Unable to provide resources with unknown resource provider");
        v.verifyTextInLog("java.lang.ClassNotFoundException: unknown");
        v.verifyTextInLog("[INFO] yang-to-resources: Resource provider instantiated from org.opendaylight.controller.yang2sources.spi.ResourceProviderTestImpl");
        v.verifyTextInLog("[INFO] yang-to-resources: Resource provider org.opendaylight.controller.yang2sources.spi.ResourceProviderTestImpl call successful");
        v.verifyTextInLog("[ERROR] yang-to-resources: One or more code resource provider failed, including failed list(resourceProviderClass=exception) {unknown=java.lang.ClassNotFoundException}");
    }

}
