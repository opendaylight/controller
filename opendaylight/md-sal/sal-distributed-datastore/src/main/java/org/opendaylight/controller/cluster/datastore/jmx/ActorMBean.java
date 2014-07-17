package org.opendaylight.controller.cluster.datastore.jmx;

/**
 * @author: syedbahm
 * Date: 7/16/14
 */
public interface ActorMBean {
  int getMailboxSize();
  String getMemberInfo();


}
