/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.cluster.datastore.model;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class TestModel {

  public static final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13",
          "test");

  public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
  public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
  public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
  public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
  public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
  public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
  public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");
  private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";

  public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
  public static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH).
          node(OUTER_LIST_QNAME).build();
  public static final YangInstanceIdentifier INNER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH).
          node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).build();
  public static final QName TWO_QNAME = QName.create(TEST_QNAME,"two");
  public static final QName THREE_QNAME = QName.create(TEST_QNAME,"three");


  public static final InputStream getDatastoreTestInputStream() {
    return getInputStream(DATASTORE_TEST_YANG);
  }

  private static InputStream getInputStream(final String resourceName) {
    return TestModel.class.getResourceAsStream(DATASTORE_TEST_YANG);
  }

  public static SchemaContext createTestContext() {
    YangParserImpl parser = new YangParserImpl();
    Set<Module> modules = parser.parseYangModelsFromStreams(Collections.singletonList(getDatastoreTestInputStream()));
    return parser.resolveSchemaContext(modules);
  }
}
