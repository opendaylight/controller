package org.opendaylight.controller.datastore.clustered.util

/**
 *
 * @author: syedbahm
 *          Date: 5/21/14
 *
 *
 *
 */
object Constant {
  def NUMBER_OF_FAMILIES: Int = 150000

  //for periodic message sending needed to reduce to not get OOM errors of 150k messages
  def NUMBER_OF_FAMILIES_PERSIST_RUN = 1

}
