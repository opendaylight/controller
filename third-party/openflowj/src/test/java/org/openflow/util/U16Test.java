
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.util;

import junit.framework.TestCase;

public class U16Test extends TestCase {
  /**
   * Tests that we correctly translate unsigned values in and out of a short
   * @throws Exception
   */
  public void test() throws Exception {
      int val = 0xffff;
      TestCase.assertEquals((short)-1, U16.t(val));
      TestCase.assertEquals((short)32767, U16.t(0x7fff));
      TestCase.assertEquals(val, U16.f((short)-1));
  }
}
