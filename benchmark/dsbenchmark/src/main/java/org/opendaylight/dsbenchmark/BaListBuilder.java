/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerListKey;

public final class BaListBuilder {
    private BaListBuilder() {

    }

    public static List<OuterList> buildOuterList(final int outerElements, final int innerElements) {
        List<OuterList> outerList = new ArrayList<>(outerElements);
        for (int j = 0; j < outerElements; j++) {
            outerList.add(new OuterListBuilder()
                .setId(j)
                .setInnerList(buildInnerList(j, innerElements))
                .withKey(new OuterListKey(j))
                .build());
        }
        return outerList;
    }

    private static Map<InnerListKey, InnerList> buildInnerList(final int index, final int elements) {
        Builder<InnerListKey, InnerList> innerList = ImmutableMap.builderWithExpectedSize(elements);

        final String itemStr = "Item-" + index + "-";
        for (int i = 0; i < elements; i++) {
            final InnerListKey key = new InnerListKey(i);
            innerList.put(key, new InnerListBuilder()
                .withKey(key)
                .setName(i)
                .setValue(itemStr + i)
                .build());
        }
        return innerList.build();
    }
}
