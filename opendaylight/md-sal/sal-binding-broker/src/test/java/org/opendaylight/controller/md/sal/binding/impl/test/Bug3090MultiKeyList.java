/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataTreeChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.Root;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.RootBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.root.ListInRoot;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.root.ListInRootBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public class Bug3090MultiKeyList extends AbstractDataTreeChangeListenerTest {
    private static final InstanceIdentifier<Root> ROOT_PATH = InstanceIdentifier.create(Root.class);

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(Root.class));
    }

    @Test
    public void listWithMultiKeyTest() {
        final List<ListInRoot> listInRoots = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            listInRoots.add(new ListInRootBuilder()
                .setLeafA("leaf a" + i)
                .setLeafC("leaf c" + i)
                .setLeafB("leaf b" + i)
                .build()
            );
        }

        final Root root = new RootBuilder().setListInRoot(listInRoots).build();

        final TestListener<Root> listener = createListener(LogicalDatastoreType.CONFIGURATION, ROOT_PATH,
                match(ModificationType.WRITE, ROOT_PATH, Objects::isNull,
                        (Function<Root, Boolean>) dataAfter -> checkData(root, dataAfter)));

        final ReadWriteTransaction readWriteTransaction = getDataBroker().newReadWriteTransaction();
        readWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, ROOT_PATH, root);
        assertCommit(readWriteTransaction.submit());

        listener.verify();
    }

    private static boolean checkData(final Root expected, final Root actual) {
        if (actual == null) {
            return false;
        }

        Set<ListInRoot> expListInRoot = new HashSet<>(expected.getListInRoot());
        Set<ListInRoot> actualListInRoot = actual.getListInRoot().stream()
                .map(list -> new ListInRootBuilder(list).build()).collect(Collectors.toSet());
        return expListInRoot.equals(actualListInRoot);
    }
}
