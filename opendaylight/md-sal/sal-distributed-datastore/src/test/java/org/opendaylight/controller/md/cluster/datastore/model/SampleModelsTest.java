/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class SampleModelsTest {
    @Test
    public void testPeopleModel(){
        NormalizedNode<?, ?> expected = PeopleModel.create();


        NormalizedNodeMessages.Container node =
            new NormalizedNodeToNodeCodec(SchemaContextHelper.full())
                .encode(InstanceIdentifier.of(PeopleModel.BASE_QNAME),
                    expected);

        NormalizedNodeMessages.Node normalizedNode =
            node.getNormalizedNode();

        NormalizedNode<?,?> actual = new NormalizedNodeToNodeCodec(SchemaContextHelper.full()).decode(InstanceIdentifier.of(PeopleModel.BASE_QNAME),
            normalizedNode);


        Assert.assertEquals(expected.toString(), actual.toString());

    }


    @Test
    public void testCarsModel(){
        NormalizedNode<?, ?> expected = CarsModel.create();


        NormalizedNodeMessages.Container node =
            new NormalizedNodeToNodeCodec(SchemaContextHelper.full())
                .encode(InstanceIdentifier.of(CarsModel.BASE_QNAME),
                    expected);

        NormalizedNodeMessages.Node normalizedNode =
            node.getNormalizedNode();

        NormalizedNode<?,?> actual = new NormalizedNodeToNodeCodec(SchemaContextHelper.full()).decode(InstanceIdentifier.of(CarsModel.BASE_QNAME),
            normalizedNode);


        Assert.assertEquals(expected.toString(), actual.toString());

    }
}
