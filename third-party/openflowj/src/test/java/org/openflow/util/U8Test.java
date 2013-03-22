
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.util;

import junit.framework.TestCase;

public class U8Test extends TestCase {
  /**
   * Tests that we correctly translate unsigned values in and out of a byte
   * @throws Exception
   */
  public void test() throws Exception {
      short val = 0xff;
      TestCase.assertEquals(-1, U8.t(val));
      TestCase.assertEquals(val, U8.f((byte)-1));
  }
}
