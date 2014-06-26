/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.test;

import junit.framework.Assert;

import org.junit.Test;
import org.opendaylight.controller.sal.compatibility.MDFlowMapping;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

/**
 * test for {@link MDFlowMapping}
 */
public class MDFlowMappingTest {

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.MDFlowMapping#toUri(org.opendaylight.controller.sal.core.NodeConnector)}.
     * @throws ConstructionException
     */
    @Test
    public void testToUri() throws ConstructionException {
        Node node = new Node(NodeIDType.OPENFLOW, 41L);
        NodeConnector connector = new NodeConnector(NodeConnectorIDType.OPENFLOW, (short) 42, node);
        Uri observed = MDFlowMapping.toUri(connector );

        Assert.assertEquals("openflow:41:42", observed.getValue());
    }

}
