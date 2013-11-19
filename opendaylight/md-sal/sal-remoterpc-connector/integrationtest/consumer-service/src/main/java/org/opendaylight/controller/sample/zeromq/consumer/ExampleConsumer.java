package org.opendaylight.controller.sample.zeromq.consumer;

import java.net.URI;
import java.util.Hashtable;
import java.util.concurrent.*;

import org.opendaylight.controller.sal.core.api.AbstractConsumer;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleConsumer extends AbstractConsumer {

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");

  private ConsumerSession session;

  private ServiceRegistration<ExampleConsumer> thisReg;
  private Logger _logger = LoggerFactory.getLogger(ExampleConsumer.class);

  @Override
  public void onSessionInitiated(ConsumerSession session) {
    this.session = session;
  }

  public RpcResult<CompositeNode> invokeRpc(){
    _logger.info("Invoking RPC");
    RpcResult<CompositeNode> result = null;
    Future<RpcResult<CompositeNode>> future = ExampleConsumer.this.session.rpc(QNAME, null);
    try {
      result = future.get();
    } catch (Exception e) {
      e.printStackTrace();
    }
    _logger.info("Returning result [{}]", result);
    return result;
  }

  @Override
  protected void startImpl(BundleContext context){
    thisReg = context.registerService(ExampleConsumer.class, this, new Hashtable<String,String>());
  }
  @Override
  protected void stopImpl(BundleContext context) {
    super.stopImpl(context);
    thisReg.unregister();
  }
}
