/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Unit tests for WriteData.
 *
 * @author Thomas Pantelis
 */
public class WriteDataTest {

    @Test
    public void testSerialization() {
        SchemaContext schemaContext = TestModel.createTestContext();
        WriteData expected = new WriteData(TestModel.TEST_PATH, ImmutableNodes
            .containerNode(TestModel.TEST_QNAME), schemaContext);

        WriteData actual = WriteData.fromSerializable(expected.toSerializable(), schemaContext);
        Assert.assertEquals("getPath", expected.getPath(), actual.getPath());
        Assert.assertEquals("getData", expected.getData(), actual.getData());
    }
}
