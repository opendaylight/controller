/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerListKey;

public final class BaListBuilder {
    public static List<OuterList> buildOuterList(int outerElements, int innerElements) {
        List<OuterList> outerList = new ArrayList<>(outerElements);
        for (int j = 0; j < outerElements; j++) {
            outerList.add(new OuterListBuilder()
                                .setId( j )
                                .setInnerList(buildInnerList(j, innerElements))
                                .setKey(new OuterListKey( j ))
                                .build());
        }
        return outerList;
    }

    private static List<InnerList> buildInnerList( int index, int elements ) {
        List<InnerList> innerList = new ArrayList<>( elements );

        final String itemStr = "Item-" + String.valueOf(index) + "-";
        for (int i = 0; i < elements; i++) {
            innerList.add(new InnerListBuilder()
                                .setKey( new InnerListKey( i ) )
                                .setName(i)
                                .setValue( itemStr + String.valueOf( i ) )
                                .build());
        }
        return innerList;
    }
}
