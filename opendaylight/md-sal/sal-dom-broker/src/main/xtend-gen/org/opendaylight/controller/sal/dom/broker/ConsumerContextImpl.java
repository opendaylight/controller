package org.opendaylight.controller.sal.dom.broker;

import com.google.common.base.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;

@SuppressWarnings("all")
public class ConsumerContextImpl implements ConsumerSession {
  private final Consumer _consumer;
  
  public Consumer getConsumer() {
    return this._consumer;
  }
  
  private BrokerImpl _broker;
  
  public BrokerImpl getBroker() {
    return this._broker;
  }
  
  public void setBroker(final BrokerImpl broker) {
    this._broker = broker;
  }
  
  private final Map<Class<? extends BrokerService>,BrokerService> instantiatedServices = new Function0<Map<Class<? extends BrokerService>,BrokerService>>() {
    public Map<Class<? extends BrokerService>,BrokerService> apply() {
      HashMap<Class<? extends BrokerService>,BrokerService> _hashMap = new HashMap<Class<? extends BrokerService>, BrokerService>();
      Map<Class<? extends BrokerService>,BrokerService> _synchronizedMap = Collections.<Class<? extends BrokerService>, BrokerService>synchronizedMap(_hashMap);
      return _synchronizedMap;
    }
  }.apply();
  
  private boolean closed = false;
  
  private BundleContext context;
  
  public ConsumerContextImpl(final Consumer consumer, final BundleContext ctx) {
    this._consumer = consumer;
    this.context = ctx;
  }
  
  public Future<RpcResult<CompositeNode>> rpc(final QName rpc, final CompositeNode input) {
    BrokerImpl _broker = this.getBroker();
    return _broker.invokeRpc(rpc, input);
  }
  
  public <T extends BrokerService> T getService(final Class<T> service) {
    final BrokerService potential = this.instantiatedServices.get(service);
    boolean _notEquals = (!Objects.equal(potential, null));
    if (_notEquals) {
      final T ret = ((T) potential);
      return ret;
    }
    BrokerImpl _broker = this.getBroker();
    final T ret_1 = _broker.<T>serviceFor(service, this);
    boolean _notEquals_1 = (!Objects.equal(ret_1, null));
    if (_notEquals_1) {
      this.instantiatedServices.put(service, ret_1);
    }
    return ret_1;
  }
  
  public void close() {
    final Collection<BrokerService> toStop = this.instantiatedServices.values();
    this.closed = true;
    for (final BrokerService brokerService : toStop) {
      brokerService.closeSession();
    }
    BrokerImpl _broker = this.getBroker();
    _broker.consumerSessionClosed(this);
  }
  
  public boolean isClosed() {
    return this.closed;
  }
}
