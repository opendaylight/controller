package org.opendaylight.controller.sample.zeromq.consumer;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.core.api.AbstractConsumer;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;

public class ExampleConsumer extends AbstractConsumer {

    private final URI namespace = URI.create("http://cisco.com/example");
    private final QName QNAME = new QName(namespace,"heartbeat");
    
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ConsumerSession session;
    
    
    @Override
    public void onSessionInitiated(ConsumerSession session) {
        this.session = session;
        executor.scheduleAtFixedRate(new Runnable() {
            
            @Override
            public void run() {
                int count = 0;
                try {
                    Future<RpcResult<CompositeNode>> future = ExampleConsumer.this.session.rpc(QNAME, null);
                    RpcResult<CompositeNode> result = future.get();
                    System.out.println("Result received. Status is :" + result.isSuccessful());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    @Override
    protected void stopImpl(BundleContext context) {
        // TODO Auto-generated method stub
        super.stopImpl(context);
        executor.shutdown();
    }
}
