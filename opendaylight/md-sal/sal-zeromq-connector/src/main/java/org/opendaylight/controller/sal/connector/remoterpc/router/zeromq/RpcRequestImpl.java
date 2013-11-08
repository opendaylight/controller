package org.opendaylight.controller.sal.connector.remoterpc.router.zeromq;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: abhishk2
 * Date: 10/25/13
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class RpcRequestImpl implements RpcRouter.RpcRequest<QName, QName, InstanceIdentifier, Object>,Serializable {

  private RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier;
  private Object payload;

  @Override
  public RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> getRoutingInformation() {
    return routeIdentifier;
  }

  public void setRouteIdentifier(RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier) {
    this.routeIdentifier = routeIdentifier;
  }

  @Override
  public Object getPayload() {
    return payload;
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

}
