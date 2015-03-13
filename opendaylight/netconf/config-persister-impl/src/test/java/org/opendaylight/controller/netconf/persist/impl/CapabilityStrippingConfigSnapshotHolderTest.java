/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl;

import static org.junit.Assert.assertEquals;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Element;

public class CapabilityStrippingConfigSnapshotHolderTest {

    @Test
    public void testCapabilityStripping() throws Exception {
        Set<String> allCapabilities = readLines("/capabilities-all.txt");
        Set<String> expectedCapabilities = readLines("/capabilities-stripped.txt");
        String snapshotAsString = readToString("/snapshot.xml");
        Element element = XmlUtil.readXmlToElement(snapshotAsString);
        CapabilityStrippingConfigSnapshotHolder tested = new CapabilityStrippingConfigSnapshotHolder(
                element, allCapabilities);
        assertEquals(expectedCapabilities, tested.getCapabilities());

        Set<String> obsoleteCapabilities = Sets.difference(allCapabilities, expectedCapabilities);

        assertEquals(obsoleteCapabilities, tested.getObsoleteCapabilities());
    }

    private Set<String> readLines(String fileName) throws IOException {
        return new HashSet<>(Resources.readLines(getClass().getResource(fileName), Charsets.UTF_8));
    }

    private String readToString(String fileName) throws IOException {
        return Resources.toString(getClass().getResource(fileName), Charsets.UTF_8);
    }

}
