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

/**
 * Unit tests for DeleteData.
 *
 * @author Thomas Pantelis
 */
public class DeleteDataTest {

    @Test
    public void testSerialization() {
        DeleteData expected = new DeleteData(TestModel.TEST_PATH);

        DeleteData actual = (DeleteData) SerializationUtils.clone(expected);
        Assert.assertEquals("getPath", expected.getPath(), actual.getPath());
    }
}
