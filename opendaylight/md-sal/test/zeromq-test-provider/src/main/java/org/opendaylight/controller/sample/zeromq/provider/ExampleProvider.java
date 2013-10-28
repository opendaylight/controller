package org.opendaylight.controller.sample.zeromq.provider;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;

public class ExampleProvider extends AbstractProvider implements RpcImplementation  {

    private final URI namespace = URI.create("http://cisco.com/example");
    private final QName QNAME = new QName(namespace,"heartbeat");
    private RpcRegistration reg;
    
    
    @Override
    public void onSessionInitiated(ProviderSession session) {
      //Adding heartbeat 10 times just to make sure subscriber get it
      for (int i=0;i<10;i++){
        System.out.println("ExampleProvider: Adding " + QNAME + " " + i);
        reg = session.addRpcImplementation(QNAME, this);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }
    
    @Override
    public Set<QName> getSupportedRpcs() {
        return Collections.singleton(QNAME);
    }
    
    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
        if(QNAME.equals(rpc)) {
            RpcResult<CompositeNode> output = Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
            return output;
        }
        RpcResult<CompositeNode> output = Rpcs.getRpcResult(false, null, Collections.<RpcError>emptySet());
        return output;
    }
    
    @Override
    protected void stopImpl(BundleContext context) {
     if(reg != null) {
         try {
            reg.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
    }

}
