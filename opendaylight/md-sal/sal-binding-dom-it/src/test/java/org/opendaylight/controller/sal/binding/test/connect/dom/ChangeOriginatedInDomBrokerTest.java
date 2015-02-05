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
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;

/**
 * FIXME: Migrate to use new Data Broker APIs
 */
@SuppressWarnings("deprecation")
public class ChangeOriginatedInDomBrokerTest extends AbstractDataServiceTest {

    protected static final Logger LOG = LoggerFactory.getLogger(ChangeOriginatedInDomBrokerTest.class);

    private static final QName NODE_ID_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final QName LVU_ID_QNAME = QName.create(ListViaUses.QNAME, "name");

    private static final String NODE_ID = "node:1";
    private static final ListViaUsesKey LVU_ID = new ListViaUsesKey("1234");

    private static final TopLevelListKey NODE_KEY = new TopLevelListKey(NODE_ID);
    private static final ListViaUsesKey LVU_KEY = new ListViaUsesKey(LVU_ID);

    protected final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> modificationCapture = SettableFuture.create();

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<TopLevelList> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .child(TopLevelList.class, NODE_KEY).build();

    private static final Map<QName, Object> LVU_KEY_BI = //
    ImmutableMap.<QName, Object> of(LVU_ID_QNAME, LVU_ID.getName());

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier LVU_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, NODE_KEY_BI) //
            .nodeWithKey(ListViaUses.QNAME, LVU_KEY_BI) //
            .build();

    private static final InstanceIdentifier<ListViaUses> LVU_PATH_BA = //
            NODE_INSTANCE_ID_BA.builder() //
            .augmentation(TreeComplexUsesAugment.class) //
            .child(ListViaUses.class) //
            .build();

    private static final InstanceIdentifier<ListViaUses> LVU_INSTANCE_ID_BA = //
            LVU_PATH_BA.firstIdentifierOf(TreeComplexUsesAugment.class)
            .child(ListViaUses.class, LVU_KEY);

    @Test
    public void simpleModifyOperation() throws Exception {
        assertNull(biDataService.readConfigurationData(LVU_INSTANCE_ID_BI));

        registerChangeListener();

        CompositeNode domLvu = createTestLvu();
        DataModificationTransaction biTransaction = biDataService.beginTransaction();
        biTransaction.putConfigurationData(LVU_INSTANCE_ID_BI, domLvu);
        RpcResult<TransactionStatus> biResult = biTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, biResult.getResult());
        DataChangeEvent<InstanceIdentifier<?>, DataObject> event = modificationCapture.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(event);
        LOG.info("Created Configuration :{}",event.getCreatedConfigurationData());
        ListViaUses lvu = (ListViaUses) event.getCreatedConfigurationData().get(LVU_INSTANCE_ID_BA);
        assertNotNull(lvu);
        assertNotNull(lvu.getName());
        assertEquals(TransactionStatus.COMMITED, biResult.getResult());

    }

    private void registerChangeListener() {
        baDataService.registerDataChangeListener(LVU_PATH_BA, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
                LOG.info("Data Change listener invoked.");
                modificationCapture.set(change);
            }
        });
    }

    private CompositeNode createTestLvu() {
        ListViaUsesBuilder lvu = new ListViaUsesBuilder();
        lvu.setKey(LVU_KEY)
            .setName("lvu-name-0");

        CompositeNode domLvu = mappingService.toDataDom(lvu.build());
        return domLvu;
    }
}
