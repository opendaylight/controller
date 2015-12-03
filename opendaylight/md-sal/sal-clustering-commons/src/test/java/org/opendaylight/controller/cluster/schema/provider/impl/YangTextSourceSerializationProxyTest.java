/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.schema.provider.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

public class YangTextSourceSerializationProxyTest {

    private YangTextSchemaSource schemaSource;

    @Before
    public void setUp() {
        String source = "Test source.";
        schemaSource = YangTextSchemaSource.delegateForByteSource(
                new SourceIdentifier("test", "2015-10-30"), ByteSource.wrap(source.getBytes(Charsets.UTF_8)));
    }


    @Test
    public void serializeAndDesrializeProxy() throws ClassNotFoundException, IOException {
        YangTextSchemaSourceSerializationProxy proxy = new YangTextSchemaSourceSerializationProxy(schemaSource);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(proxy);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));

        YangTextSchemaSourceSerializationProxy deserializedProxy =  (YangTextSchemaSourceSerializationProxy) ois.readObject();

        assertEquals(deserializedProxy.getRepresentation().getIdentifier(), proxy.getRepresentation().getIdentifier());
        assertArrayEquals(deserializedProxy.getRepresentation().read(), proxy.getRepresentation().read());
    }

    @Test
    public void testProxyEqualsBackingYangTextSource() throws IOException {
        YangTextSchemaSourceSerializationProxy serializationProxy = new YangTextSchemaSourceSerializationProxy(schemaSource);

        assertEquals(serializationProxy.getRepresentation().getIdentifier(), schemaSource.getIdentifier());
        assertArrayEquals(serializationProxy.getRepresentation().read(), schemaSource.read());
    }
}
