package org.opendaylight.controller.datastore.clustered.persistence.model

/**
 *
 *
 *
 **/

import scala.collection.mutable.HashMap

import scala.collection.immutable.TreeMap
import org.opendaylight.controller.datastore.clustered.util.Constant

class Families extends Serializable {


  var families = new TreeMap[String, HashMap[String, Any]]

  def addAFamily(familyId: Long, numOfChildren: Int, numOfGrandChildren: Int): TreeMap[String, HashMap[String, Any]] = {


    families += ("/family/" + familyId -> HashMap("name" -> "family".concat(familyId.toString()), "age" -> familyId / 100))
    var x = 0;
    var gc = 0;
    var y = 0;
    for (x <- 1 to numOfChildren) {
      val child = "/family/" + familyId + "/child/" + x;
      families += (child -> HashMap("name" -> "child".concat(x.toString()), "age" -> (x + 20) % x));

      for (y <- 1 to numOfGrandChildren) {
        gc = gc + 1;
        val grandchild = child + "/grandchild/" + gc;
        families += (grandchild -> HashMap("name" -> (familyId.toString + ":grandchild".concat(gc.toString())), "age" -> 5 % gc));

      }
    }
    families
  }

  def addChildrenToAFamily(familyId: Long, numOfChildren: Int, numOfGrandChildren: Int) {
    if (!families.contains("/family/" + familyId)) {
      addAFamily(familyId, numOfChildren, numOfGrandChildren);
    } else {
      //get the current child count
      val children: TreeMap[String, HashMap[String, Any]] = families.range("family/" + familyId + "/child/", "family/" + familyId + "/child/" + 1 + "/grandchild/");
      val grandChildren: TreeMap[String, HashMap[String, Any]] = families.from("family/" + familyId + "/child/" + numOfChildren + "/grandchild/");
      var gc = 0;
      var x = 0;
      var y = 0;
      for (x <- children.size to numOfChildren + children.size) {
        val child = "/family/" + familyId + "/child/" + x;
        families += (child -> HashMap("name" -> "child".concat(x.toString()), "age" -> (x + 20) % x));

        for (y <- grandChildren.size + 1 to numOfGrandChildren + grandChildren.size + 1) {
          gc = gc + 1;
          val grandchild = child + "/grandchild/" + gc;
          families += (grandchild -> HashMap("name" -> "grandchild".concat(gc.toString()), "age" -> 5 % gc));

        }
      }

    }

  }

  def addAFamily(singleFamily: TreeMap[String, HashMap[String, Any]]): TreeMap[String, HashMap[String, Any]] = {

    singleFamily.-("MsgId");
    families = families ++ singleFamily.toList
    families
  }

  def getFamilies: TreeMap[String, HashMap[String, Any]] = families;


  def resetFamilies: Unit = {
    families = TreeMap.empty[String, HashMap[String, Any]]
  }


}
