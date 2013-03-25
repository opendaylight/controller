package org.openflow.util;

import junit.framework.TestCase;

public class U32Test extends TestCase {
  /**
   * Tests that we correctly translate unsigned values in and out of an int
   * @throws Exception
   */
  public void test() throws Exception {
      long val = 0xffffffffL;
      TestCase.assertEquals(-1, U32.t(val));
      TestCase.assertEquals(val, U32.f(-1));
  }
}
