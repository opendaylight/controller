/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;


import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.fail;

public class NormalizedNodeStreamReaderWriterTest {

    final NormalizedNode<?, ?> input = TestModel.createTestContainer();

    @Test
    public void testNormalizedNodeStreamReaderWriter() {

        byte[] byteData = null;

        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            NormalizedNodeStreamWriter writer = new NormalizedNodeOutputStreamWriter(byteArrayOutputStream)) {

            NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(writer);
            normalizedNodeWriter.write(input);
            byteData = byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            fail("Writing to OutputStream failed :" + e.toString());
        }

        try(NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(new ByteArrayInputStream(byteData))) {

            NormalizedNode<?,?> node = reader.readNormalizedNode();
            Assert.assertEquals(input, node);

        } catch (IOException e) {
            fail("Reading from InputStream failed :" + e.toString());
        }
    }

    @Test
    public void testWithSerializable() {
        SampleNormalizedNodeSerializable serializable = new SampleNormalizedNodeSerializable(input);
        SampleNormalizedNodeSerializable clone = (SampleNormalizedNodeSerializable)SerializationUtils.clone(serializable);

        Assert.assertEquals(input, clone.getInput());

    }

}
