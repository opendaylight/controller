/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * FIXME: Migrate to use new Data Broker APIs
 */
@SuppressWarnings("deprecation")
public class ChangeOriginatedInDomBrokerTest extends AbstractDataServiceTest {

    protected static final Logger LOG = LoggerFactory.getLogger(ChangeOriginatedInDomBrokerTest.class);

    private static final QName TLL_NAME_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final QName LIST1_ATTR_STR_QNAME = QName.create(List1.QNAME, "attr-str");

    private static final String TLL_NAME = "1";
    private static final int LIST11_ATTR_INT = 1234;
    private static final String LIST1_ATTR_STR = "foo:foo";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);
    private static final List1Key LIST1_KEY = new List1Key(LIST1_ATTR_STR);
    private static final List11Key LIST11_KEY = new List11Key(LIST11_ATTR_INT);

    protected final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> modificationCapture = SettableFuture.create();

    private static final Map<QName, Object> TLL_KEY_BI = Collections.<QName, Object> singletonMap(TLL_NAME_QNAME,
            TLL_NAME);

    private static final InstanceIdentifier<TopLevelList> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .child(TopLevelList.class, TLL_KEY).toInstance();

    private static final Map<QName, Object> LIST1_KEY_BI = //
    ImmutableMap.<QName, Object> of(LIST1_ATTR_STR_QNAME, LIST1_ATTR_STR);;

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier LIST1_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, TLL_KEY_BI) //
            .nodeWithKey(List1.QNAME, LIST1_KEY_BI) //
            .toInstance();

    private static final InstanceIdentifier<List1> LIST1_PATH_BA = //
            NODE_INSTANCE_ID_BA.builder() //
            .augmentation(TllComplexAugment.class) //
            .child(List1.class, LIST1_KEY) //
            .toInstance();

    @Test
    public void simpleModifyOperation() throws Exception {
        //TODO: Implement with NormalizedNodes
    }
}
