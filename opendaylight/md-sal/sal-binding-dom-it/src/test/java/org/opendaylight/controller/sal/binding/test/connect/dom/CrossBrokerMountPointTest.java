/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.List11SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.top.top.level.list.list1.list1._1.Cont;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

@SuppressWarnings("deprecation")
public class CrossBrokerMountPointTest {

    private static final QName TLL_NAME_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final String TLL_NAME = "foo:1";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);

    private static final Map<QName, Object> TLL_KEY_BI = Collections.<QName, Object> singletonMap(TLL_NAME_QNAME,
            TLL_NAME);

    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .child(TopLevelList.class, TLL_KEY).build();

    private static final List1Key LIST1_KEY = new List1Key("foo");
    private static final List11Key LIST11_KEY = new List11Key(1);

    private static final InstanceIdentifier<Cont> AUG_CONT_ID_BA = TLL_INSTANCE_ID_BA
            .builder().augmentation(TllComplexAugment.class) //
            .child(List1.class, LIST1_KEY) //
            .child(List11.class, LIST11_KEY) //
            .augmentation(List11SimpleAugment.class) //
            .child(Cont.class) //
            .build();

    private static final QName AUG_CONT = QName.create(List11.QNAME,
            Cont.QNAME.getLocalName());

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier TLL_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .node(TopLevelList.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, TLL_KEY_BI) //
            .build();

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier GROUP_STATISTICS_ID_BI = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            //
            .builder(TLL_INSTANCE_ID_BI)
            .nodeWithKey(QName.create(TllComplexAugment.QNAME, "list1"), QName.create(TllComplexAugment.QNAME, "attr-str"),
                    LIST1_KEY.getAttrStr())
            .nodeWithKey(QName.create(TllComplexAugment.QNAME, "list1-1"), QName.create(TllComplexAugment.QNAME, "attr-int"),
                    LIST11_KEY.getAttrInt())
            .node(AUG_CONT).build();

    private BindingTestContext testContext;
    private MountProviderService bindingMountPointService;
    private DOMMountPointService domMountPointService;

    @Before
    public void setup() {
        final BindingBrokerTestFactory testFactory = new BindingBrokerTestFactory();
        testFactory.setExecutor(MoreExecutors.sameThreadExecutor());
        testFactory.setStartWithParsedSchema(true);
        testContext = testFactory.getTestContext();

        testContext.start();
        bindingMountPointService = testContext.getBindingMountProviderService();
        domMountPointService = testContext.getDomMountProviderService();

        // biRpcInvoker = testContext.getDomRpcInvoker();
        assertNotNull(bindingMountPointService);
        assertNotNull(domMountPointService);

        // flowService = MessageCapturingFlowService.create(baRpcRegistry);
    }

    @Test
    public void testMountPoint() {
        final Integer attrIntValue = 500;
        domMountPointService.createMountPoint(TLL_INSTANCE_ID_BI)
            .addService(DOMDataBroker.class, new DOMDataBroker() {

                @Override
                public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
                        final YangInstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DOMDataWriteTransaction newWriteOnlyTransaction() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DOMDataReadWriteTransaction newReadWriteTransaction() {
                    return  new DOMDataReadWriteTransaction() {

                        @Override
                        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                                final LogicalDatastoreType store, final YangInstanceIdentifier path) {
                            if(store == LogicalDatastoreType.OPERATIONAL && path.getLastPathArgument().equals(GROUP_STATISTICS_ID_BI.getLastPathArgument())) {

                                final ContainerNode data = Builders.containerBuilder()
                                        .withNodeIdentifier(new NodeIdentifier(AUG_CONT))
                                        .withChild(ImmutableNodes.leafNode(QName.create(AUG_CONT, "attr-int"), attrIntValue))
                                        .build();

                                return Futures.immediateCheckedFuture(Optional.<NormalizedNode<?,?>>of(data));
                            }
                            return Futures.immediateFailedCheckedFuture(new ReadFailedException(TLL_NAME, new Exception()));
                        }

                        @Override
                        public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                                final YangInstanceIdentifier path) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public Object getIdentifier() {
                            return this;
                        }

                        @Override
                        public boolean cancel() {
                            return false;
                        }

                        @Override
                        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                            return null;
                        }

                        @Override
                        public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                                final NormalizedNode<?, ?> data) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                                final NormalizedNode<?, ?> data) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                            throw new UnsupportedOperationException();
                        }

                    };
                }

                @Override
                public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
                    throw new UnsupportedOperationException();
                }
            }).register();



        final MountProviderInstance bindingMountPoint = bindingMountPointService.getMountPoint(TLL_INSTANCE_ID_BA);
        assertNotNull(bindingMountPoint);

        final Cont data = (Cont) bindingMountPoint.readOperationalData(AUG_CONT_ID_BA);
        assertNotNull(data);
        assertEquals(attrIntValue ,data.getAttrInt());
    }
}
