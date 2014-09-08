/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.DataOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.TwoLevelListProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class BindingTwoListWriter extends AbstractTwoLevelListWriter<InstanceIdentifier<?>, DataObject, List<NestedList>> {

    public BindingTwoListWriter(TwoLevelListProperties input, DataOperation writeOperation) {
        super(input,TransactionWriter.BindingTransactionWriter.from(writeOperation));
    }

    @Override
    List<NestedList> createInnerList(long innerItems) {
        List<NestedList> list = new ArrayList<NestedList>((int) innerItems);
        for(int i =0;i<innerItems;i++) {
            NestedListBuilder builder = new NestedListBuilder();
            builder.setKey(new NestedListKey(String.valueOf(i)));
            builder.setType("binding");
            list.add(builder.build());
        }
        return list;
    }

    @Override
    DataObject createOuterListItem(long id, List<NestedList> innerList) {
        return new TopLevelListBuilder()
        .setKey(new TopLevelListKey(String.valueOf(id)))
        .setNestedList(innerList)
        .build();
    }

    @Override
    InstanceIdentifier<?> createOuterListItemPath(long id) {
        TopLevelListKey listKey = new TopLevelListKey(String.valueOf(id));
        return InstanceIdentifier.create(Top.class).child(TopLevelList.class, listKey);
    }

}