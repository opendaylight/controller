/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.codehaus.jackson.JsonParseException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class RouteIdentifierImplTest {

  Logger _logger = LoggerFactory.getLogger(RouteIdentifierImplTest.class);

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");

  @Test
  public void testToString() throws Exception {
    RouteIdentifierImpl rId = new RouteIdentifierImpl();
    rId.setType(QNAME);

    _logger.debug(rId.toString());

    Assert.assertTrue(true);

  }

  @Test
  public void testFromString() throws Exception {
    RouteIdentifierImpl rId = new RouteIdentifierImpl();
    rId.setType(QNAME);

    _logger.debug("route: " + rId.fromString(rId.toString()));

    Assert.assertTrue(true);
  }

  @Test(expected = JsonParseException.class)
  public void testFromInvalidString() throws Exception {
    String invalidInput = "aklhdgadfa;;;;;;;]]]]=]ag" ;
    RouteIdentifierImpl rId = new RouteIdentifierImpl();
    rId.fromString(invalidInput);

    _logger.debug("" + rId);
    Assert.assertTrue(true);
  }
}
