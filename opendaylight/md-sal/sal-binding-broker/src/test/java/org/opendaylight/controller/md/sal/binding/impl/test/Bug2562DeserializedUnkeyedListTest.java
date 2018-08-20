/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataTreeChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.Root;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.RootBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.Fooroot;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.FoorootBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.fooroot.Barroot;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.fooroot.BarrootBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.fooroot.BarrootKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public class Bug2562DeserializedUnkeyedListTest extends AbstractDataTreeChangeListenerTest {
    private static final InstanceIdentifier<Root> ROOT_PATH = InstanceIdentifier.create(Root.class);

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(Root.class));
    }

    @Test
    public void writeListToList2562Root() {
        final Barroot barRoot = new BarrootBuilder().setType(2).setValue(2).withKey(new BarrootKey(2)).build();
        final Fooroot fooRoot = new FoorootBuilder().setBarroot(Arrays.asList(barRoot)).build();
        final Root root = new RootBuilder().setFooroot(Arrays.asList(fooRoot)).build();

        final TestListener<Root> listenerRoot = createListener(LogicalDatastoreType.CONFIGURATION, ROOT_PATH,
                added(ROOT_PATH, root));

        final ReadWriteTransaction readWriteTransaction = getDataBroker().newReadWriteTransaction();
        readWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, ROOT_PATH, root);
        assertCommit(readWriteTransaction.submit());

        listenerRoot.verify();
    }
}
