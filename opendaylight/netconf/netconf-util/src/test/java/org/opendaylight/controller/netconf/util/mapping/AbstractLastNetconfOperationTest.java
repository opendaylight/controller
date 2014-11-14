/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AbstractLastNetconfOperationTest {
    class LastNetconfOperationImplTest extends  AbstractLastNetconfOperation  {

        boolean handleWithNoSubsequentOperationsRun;

        protected LastNetconfOperationImplTest(String netconfSessionIdForReporting) {
            super(netconfSessionIdForReporting);
            handleWithNoSubsequentOperationsRun = false;
        }

        @Override
        protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws NetconfDocumentedException {
            handleWithNoSubsequentOperationsRun = true;
            return null;
        }

        @Override
        protected String getOperationName() {
            return "";
        }
    }

    LastNetconfOperationImplTest netconfOperation;

    @Before
    public void setUp() throws Exception {
        netconfOperation = new LastNetconfOperationImplTest("");
    }

    @Test
    public void testNetconfOperation() throws Exception {
        netconfOperation.handleWithNoSubsequentOperations(null, null);
        assertTrue(netconfOperation.handleWithNoSubsequentOperationsRun);
        assertEquals(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY, netconfOperation.getHandlingPriority());
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testHandle() throws Exception {
        NetconfOperationChainedExecution operation = mock(NetconfOperationChainedExecution.class);
        doReturn("").when(operation).toString();

        doReturn(false).when(operation).isExecutionTermination();
        netconfOperation.handle(null, null, operation);
    }
}
