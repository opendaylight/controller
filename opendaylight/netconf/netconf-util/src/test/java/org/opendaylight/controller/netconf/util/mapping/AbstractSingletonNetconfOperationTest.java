/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.mapping;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AbstractSingletonNetconfOperationTest {
    class SingletonNCOperationImpl extends AbstractSingletonNetconfOperation {

        protected SingletonNCOperationImpl(String netconfSessionIdForReporting) {
            super(netconfSessionIdForReporting);
        }

        @Override
        protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws DocumentedException{
            return null;
        }

        @Override
        protected String getOperationName() {
            return null;
        }
    }

    @Test
    public void testAbstractSingletonNetconfOperation() throws Exception {
        SingletonNCOperationImpl operation = new SingletonNCOperationImpl("");
        assertEquals(operation.getHandlingPriority(), HandlingPriority.HANDLE_WITH_MAX_PRIORITY);
    }
}
