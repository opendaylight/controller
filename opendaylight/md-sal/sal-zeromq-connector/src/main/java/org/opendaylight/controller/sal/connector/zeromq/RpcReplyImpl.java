package org.opendaylight.controller.sal.connector.zeromq;

import org.opendaylight.controller.sal.connector.api.RpcRouter;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: abhishk2
 * Date: 10/24/13
 * Time: 4:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class RpcReplyImpl implements RpcRouter.RpcReply<Object>,Serializable {

  private Object payload;

  @Override
  public Object getPayload() {
    return payload;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setPayload(Object payload){
    this.payload = payload;
  }
}
