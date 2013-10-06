/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import com.google.common.base.Objects;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.controller.sal.dom.broker.ConsumerContextImpl;
import org.opendaylight.controller.sal.dom.broker.ProviderContextImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class BrokerImpl implements Broker {
  private final static Logger log = new Function0<Logger>() {
    public Logger apply() {
      Logger _logger = LoggerFactory.getLogger(BrokerImpl.class);
      return _logger;
    }
  }.apply();
  
  private final Set<ConsumerContextImpl> sessions = new Function0<Set<ConsumerContextImpl>>() {
    public Set<ConsumerContextImpl> apply() {
      HashSet<ConsumerContextImpl> _hashSet = new HashSet<ConsumerContextImpl>();
      Set<ConsumerContextImpl> _synchronizedSet = Collections.<ConsumerContextImpl>synchronizedSet(_hashSet);
      return _synchronizedSet;
    }
  }.apply();
  
  private final Set<ProviderContextImpl> providerSessions = new Function0<Set<ProviderContextImpl>>() {
    public Set<ProviderContextImpl> apply() {
      HashSet<ProviderContextImpl> _hashSet = new HashSet<ProviderContextImpl>();
      Set<ProviderContextImpl> _synchronizedSet = Collections.<ProviderContextImpl>synchronizedSet(_hashSet);
      return _synchronizedSet;
    }
  }.apply();
  
  private final Set<BrokerModule> modules = new Function0<Set<BrokerModule>>() {
    public Set<BrokerModule> apply() {
      HashSet<BrokerModule> _hashSet = new HashSet<BrokerModule>();
      Set<BrokerModule> _synchronizedSet = Collections.<BrokerModule>synchronizedSet(_hashSet);
      return _synchronizedSet;
    }
  }.apply();
  
  private final Map<Class<? extends BrokerService>,BrokerModule> serviceProviders = new Function0<Map<Class<? extends BrokerService>,BrokerModule>>() {
    public Map<Class<? extends BrokerService>,BrokerModule> apply() {
      HashMap<Class<? extends BrokerService>,BrokerModule> _hashMap = new HashMap<Class<? extends BrokerService>, BrokerModule>();
      Map<Class<? extends BrokerService>,BrokerModule> _synchronizedMap = Collections.<Class<? extends BrokerService>, BrokerModule>synchronizedMap(_hashMap);
      return _synchronizedMap;
    }
  }.apply();
  
  private final Map<QName,RpcImplementation> rpcImpls = new Function0<Map<QName,RpcImplementation>>() {
    public Map<QName,RpcImplementation> apply() {
      HashMap<QName,RpcImplementation> _hashMap = new HashMap<QName, RpcImplementation>();
      Map<QName,RpcImplementation> _synchronizedMap = Collections.<QName, RpcImplementation>synchronizedMap(_hashMap);
      return _synchronizedMap;
    }
  }.apply();
  
  private ExecutorService _executor;
  
  public ExecutorService getExecutor() {
    return this._executor;
  }
  
  public void setExecutor(final ExecutorService executor) {
    this._executor = executor;
  }
  
  private BundleContext _bundleContext;
  
  public BundleContext getBundleContext() {
    return this._bundleContext;
  }
  
  public void setBundleContext(final BundleContext bundleContext) {
    this._bundleContext = bundleContext;
  }
  
  public ConsumerSession registerConsumer(final Consumer consumer, final BundleContext ctx) {
    this.checkPredicates(consumer);
    String _plus = ("Registering consumer " + consumer);
    BrokerImpl.log.info(_plus);
    final ConsumerContextImpl session = this.newSessionFor(consumer, ctx);
    consumer.onSessionInitiated(session);
    this.sessions.add(session);
    return session;
  }
  
  public ProviderSession registerProvider(final Provider provider, final BundleContext ctx) {
    this.checkPredicates(provider);
    final ProviderContextImpl session = this.newSessionFor(provider, ctx);
    provider.onSessionInitiated(session);
    this.providerSessions.add(session);
    return session;
  }
  
  public void addModule(final BrokerModule module) {
    String _plus = ("Registering broker module " + module);
    BrokerImpl.log.info(_plus);
    boolean _contains = this.modules.contains(module);
    if (_contains) {
      BrokerImpl.log.error("Module already registered");
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException("Module already exists.");
      throw _illegalArgumentException;
    }
    final Set<Class<? extends BrokerService>> provServices = module.getProvidedServices();
    for (final Class<? extends BrokerService> serviceType : provServices) {
      {
        String _canonicalName = serviceType.getCanonicalName();
        String _plus_1 = ("  Registering session service implementation: " + _canonicalName);
        BrokerImpl.log.info(_plus_1);
        this.serviceProviders.put(serviceType, module);
      }
    }
  }
  
  public <T extends BrokerService> T serviceFor(final Class<T> service, final ConsumerContextImpl session) {
    final BrokerModule prov = this.serviceProviders.get(service);
    boolean _equals = Objects.equal(prov, null);
    if (_equals) {
      String _string = service.toString();
      String _plus = ("Service " + _string);
      String _plus_1 = (_plus + " is not supported");
      BrokerImpl.log.warn(_plus_1);
      return null;
    }
    return prov.<T>getServiceForSession(service, session);
  }
  
  protected void addRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
    RpcImplementation _get = this.rpcImpls.get(rpcType);
    boolean _notEquals = (!Objects.equal(_get, null));
    if (_notEquals) {
      String _plus = ("Implementation for rpc " + rpcType);
      String _plus_1 = (_plus + " is already registered.");
      IllegalStateException _illegalStateException = new IllegalStateException(_plus_1);
      throw _illegalStateException;
    }
    this.rpcImpls.put(rpcType, implementation);
  }
  
  protected void removeRpcImplementation(final QName rpcType, final RpcImplementation implToRemove) {
    RpcImplementation _get = this.rpcImpls.get(rpcType);
    boolean _equals = Objects.equal(implToRemove, _get);
    if (_equals) {
      this.rpcImpls.remove(rpcType);
    }
  }
  
  protected Future<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode input) {
    final RpcImplementation impl = this.rpcImpls.get(rpc);
    ExecutorService _executor = this.getExecutor();
    final Function0<RpcResult<CompositeNode>> _function = new Function0<RpcResult<CompositeNode>>() {
      public RpcResult<CompositeNode> apply() {
        RpcResult<CompositeNode> _invokeRpc = impl.invokeRpc(rpc, input);
        return _invokeRpc;
      }
    };
    final Future<RpcResult<CompositeNode>> result = _executor.<RpcResult<CompositeNode>>submit(((Callable<RpcResult<CompositeNode>>) new Callable<RpcResult<CompositeNode>>() {
        public RpcResult<CompositeNode> call() {
          return _function.apply();
        }
    }));
    return result;
  }
  
  private void checkPredicates(final Provider prov) {
    boolean _equals = Objects.equal(prov, null);
    if (_equals) {
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException("Provider should not be null.");
      throw _illegalArgumentException;
    }
    for (final ProviderContextImpl session : this.providerSessions) {
      Provider _provider = session.getProvider();
      boolean _equals_1 = prov.equals(_provider);
      if (_equals_1) {
        IllegalStateException _illegalStateException = new IllegalStateException("Provider already registered");
        throw _illegalStateException;
      }
    }
  }
  
  private void checkPredicates(final Consumer cons) {
    boolean _equals = Objects.equal(cons, null);
    if (_equals) {
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException("Consumer should not be null.");
      throw _illegalArgumentException;
    }
    for (final ConsumerContextImpl session : this.sessions) {
      Consumer _consumer = session.getConsumer();
      boolean _equals_1 = cons.equals(_consumer);
      if (_equals_1) {
        IllegalStateException _illegalStateException = new IllegalStateException("Consumer already registered");
        throw _illegalStateException;
      }
    }
  }
  
  private ConsumerContextImpl newSessionFor(final Consumer provider, final BundleContext ctx) {
    ConsumerContextImpl _consumerContextImpl = new ConsumerContextImpl(provider, ctx);
    final ConsumerContextImpl ret = _consumerContextImpl;
    ret.setBroker(this);
    return ret;
  }
  
  private ProviderContextImpl newSessionFor(final Provider provider, final BundleContext ctx) {
    ProviderContextImpl _providerContextImpl = new ProviderContextImpl(provider, ctx);
    final ProviderContextImpl ret = _providerContextImpl;
    ret.setBroker(this);
    return ret;
  }
  
  protected void consumerSessionClosed(final ConsumerContextImpl consumerContextImpl) {
    this.sessions.remove(consumerContextImpl);
    this.providerSessions.remove(consumerContextImpl);
  }
}
