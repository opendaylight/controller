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

/**
 * Unit tests for DataExistsReply.
 *
 * @author Thomas Pantelis
 */
public class DataExistsReplyTest {

    @Test
    public void testSerialization() {
        DataExistsReply expected = new DataExistsReply(true);

        DataExistsReply actual = (DataExistsReply) SerializationUtils.clone(expected);
        Assert.assertEquals("exists", expected.exists(), actual.exists());
    }
}
