/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class SampleModelsTest {
    @Test
    public void testPeopleModel(){
        final NormalizedNode<?, ?> expected = PeopleModel.create();


        final NormalizedNodeMessages.Container node =
            new NormalizedNodeToNodeCodec(SchemaContextHelper.full())
                .encode(expected);

        final NormalizedNodeMessages.Node normalizedNode =
            node.getNormalizedNode();

        final NormalizedNode<?,?> actual = new NormalizedNodeToNodeCodec(SchemaContextHelper.full()).decode(normalizedNode);


        Assert.assertEquals(expected, actual);

    }


    @Test
    public void testCarsModel(){
        final NormalizedNode<?, ?> expected = CarsModel.create();


        final NormalizedNodeMessages.Container node =
            new NormalizedNodeToNodeCodec(SchemaContextHelper.full())
                .encode(expected);

        final NormalizedNodeMessages.Node normalizedNode =
            node.getNormalizedNode();

        final NormalizedNode<?,?> actual = new NormalizedNodeToNodeCodec(SchemaContextHelper.full()).decode(
            normalizedNode);


        Assert.assertEquals(expected, actual);

    }
}
