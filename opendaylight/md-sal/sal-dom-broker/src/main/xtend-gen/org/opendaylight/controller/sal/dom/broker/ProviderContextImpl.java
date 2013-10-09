package org.opendaylight.controller.sal.dom.broker;

import com.google.common.base.Objects;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.ConsumerContextImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.osgi.framework.BundleContext;

@SuppressWarnings("all")
public class ProviderContextImpl extends ConsumerContextImpl implements ProviderSession {
  private final Provider _provider;
  
  public Provider getProvider() {
    return this._provider;
  }
  
  private final Map<QName,RpcImplementation> rpcImpls = new Function0<Map<QName,RpcImplementation>>() {
    public Map<QName,RpcImplementation> apply() {
      HashMap<QName,RpcImplementation> _hashMap = new HashMap<QName, RpcImplementation>();
      Map<QName,RpcImplementation> _synchronizedMap = Collections.<QName, RpcImplementation>synchronizedMap(_hashMap);
      return _synchronizedMap;
    }
  }.apply();
  
  public ProviderContextImpl(final Provider provider, final BundleContext ctx) {
    super(null, ctx);
    this._provider = provider;
  }
  
  public RpcRegistration addRpcImplementation(final QName rpcType, final RpcImplementation implementation) throws IllegalArgumentException {
    boolean _equals = Objects.equal(rpcType, null);
    if (_equals) {
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException("rpcType must not be null");
      throw _illegalArgumentException;
    }
    boolean _equals_1 = Objects.equal(implementation, null);
    if (_equals_1) {
      IllegalArgumentException _illegalArgumentException_1 = new IllegalArgumentException("Implementation must not be null");
      throw _illegalArgumentException_1;
    }
    BrokerImpl _broker = this.getBroker();
    _broker.addRpcImplementation(rpcType, implementation);
    this.rpcImpls.put(rpcType, implementation);
    return null;
  }
  
  public RpcImplementation removeRpcImplementation(final QName rpcType, final RpcImplementation implToRemove) throws IllegalArgumentException {
    RpcImplementation _xblockexpression = null;
    {
      final RpcImplementation localImpl = this.rpcImpls.get(rpcType);
      boolean _notEquals = (!Objects.equal(localImpl, implToRemove));
      if (_notEquals) {
        IllegalStateException _illegalStateException = new IllegalStateException(
          "Implementation was not registered in this session");
        throw _illegalStateException;
      }
      BrokerImpl _broker = this.getBroker();
      _broker.removeRpcImplementation(rpcType, implToRemove);
      RpcImplementation _remove = this.rpcImpls.remove(rpcType);
      _xblockexpression = (_remove);
    }
    return _xblockexpression;
  }
  
  public RoutedRpcRegistration addMountedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
    UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException("TODO: auto-generated method stub");
    throw _unsupportedOperationException;
  }
  
  public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
    UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException("TODO: auto-generated method stub");
    throw _unsupportedOperationException;
  }
}
