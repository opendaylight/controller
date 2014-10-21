/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.stream;


import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

public class DDSNormalizedNodeStreamTest {

    @Test
    public void testNormalizedNodeStreamWriter() {

        final NormalizedNode<?, ?> input = TestModel.createTestContainer();

        try(NormalizedNodeStreamWriter writer = new DDSNormalizedNodeStreamWriter(new FileOutputStream("objectTest"))) {

            NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(writer);
            normalizedNodeWriter.write(input);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(DDSNormalizedNodeStreamReader reader = new DDSNormalizedNodeStreamReader(new FileInputStream("objectTest"))) {

            NormalizedNode<?,?> node = reader.readNormalizedNode();
            Assert.assertEquals(input, node);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

}
