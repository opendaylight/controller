/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

/**
 * Benchmark Model class loads the odl-datastore-test.yang model from resources.
 * <br>
 * This class serves as facilitator class which holds several references to initialized yang model as static final
 * members.
 *
 * @author Lukas Sedlak
 */
public final class BenchmarkModel {

    public static final QName TEST_QNAME = QName
        .create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13","test");
    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    public static final YangInstanceIdentifier OUTER_LIST_PATH =
            YangInstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME).build();

    private BenchmarkModel() {
    }

    public static EffectiveModelContext createTestContext() {
        return YangParserTestUtils.parseYangResources(BenchmarkModel.class, DATASTORE_TEST_YANG);
    }
}
