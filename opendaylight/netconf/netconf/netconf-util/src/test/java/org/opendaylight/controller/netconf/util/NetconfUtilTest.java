/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

public class NetconfUtilTest {

    @Test
    public void testConflictingVersionDetection() throws Exception {
        Document document = XmlUtil.readXmlToDocument(getClass().getResourceAsStream("/netconfMessages/conflictingversion/conflictingVersionResponse.xml"));
        try{
            NetconfUtil.checkIsMessageOk(document);
            fail();
        }catch(IllegalStateException e){
            assertThat(e.getMessage(), containsString("Optimistic lock failed. Expected parent version 21, was 18"));
        }
    }
}
