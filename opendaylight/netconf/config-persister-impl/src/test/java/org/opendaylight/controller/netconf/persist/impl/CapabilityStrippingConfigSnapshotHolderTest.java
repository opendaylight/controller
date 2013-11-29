/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.controller.netconf.persist.impl.osgi.ConfigPersisterActivator;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class CapabilityStrippingConfigSnapshotHolderTest {

    @Test
    public void  testCapabilityStripping() throws Exception {
        Set<String> allCapabilities = readLines("/capabilities-all.txt");
        Set<String> expectedCapabilities = readLines("/capabilities-stripped.txt");
        String snapshotAsString = readToString("/snapshot.xml");
        Element element = XmlUtil.readXmlToElement(snapshotAsString);
        {
            CapabilityStrippingConfigSnapshotHolder tested = new CapabilityStrippingConfigSnapshotHolder(
                    element, allCapabilities, Pattern.compile(
                    ConfigPersisterActivator.DEFAULT_IGNORED_REGEX
            ));
            assertEquals(expectedCapabilities, tested.getCapabilities());
            assertEquals(Collections.emptySet(), tested.getMissingNamespaces());
        }
        {
            // test regex
            CapabilityStrippingConfigSnapshotHolder tested = new CapabilityStrippingConfigSnapshotHolder(
                    element, allCapabilities, Pattern.compile(
                    "^bar"
            ));
            assertEquals(expectedCapabilities, tested.getCapabilities());
            assertEquals(Sets.newHashSet(ConfigPersisterActivator.DEFAULT_IGNORED_REGEX.substring(1)),
                    tested.getMissingNamespaces());
        }
    }

    private Set<String> readLines(String fileName) throws IOException {
        return new HashSet<>(IOUtils.readLines(getClass().getResourceAsStream(fileName)));
    }

    private String readToString(String fileName) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(fileName));
    }

}
