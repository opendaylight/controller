package org.opendaylight.controller.datastore.clustered.persistence.model

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 *
 * @author: syedbahm
 *
 *
 *
 *
 */
//@RunWith(classOf[JUnitRunner])
class FamiliesTest extends FunSuite {

  test("Should have only 1 family") {

    val families = new Families().addAFamily(1, 0, 0);
    assert(families.size == 1);
  }


  test("Should have 1 families with 3 children and 2 grand children each") {

    assert(new Families().addAFamily(1000, 3, 2).size == 1 + 3 + 3 * 2);
  }

  test("Create 1000 families with 3 children and 3 grand children each") {
    val families = new Families();
    for (x <- 1 to 1000) {
      families.addAFamily(x, 3, 2);
    }
    assert(families.getFamilies.size == 1000 + 3 * 1000 + 3 * 2 * 1000);
  }

}
