/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for ReadDataReply.
 *
 * @author Thomas Pantelis
 */
public class ReadDataReplyTest {

    @Test
    public void testSerializationWithNodePresent() {
        ReadDataReply expected = new ReadDataReply(
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build());

        ReadDataReply actual = (ReadDataReply) SerializationUtils.clone(expected);
        Assert.assertEquals("getNormalizedNode", expected.getNormalizedNode(), actual.getNormalizedNode());
    }

    @Test
    public void testSerializationWithNodeNodePresent() {
        ReadDataReply expected = new ReadDataReply(null);

        ReadDataReply actual = (ReadDataReply) SerializationUtils.clone(expected);
        Assert.assertNull("Expected null", actual.getNormalizedNode());
    }
}
