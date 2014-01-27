/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.zeromq.provider;

import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

public class ExampleProvider extends AbstractProvider implements RpcImplementation {

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");
  private RpcRegistration reg;

  private ServiceRegistration thisReg;

  private ProviderSession session;
  private Logger _logger = LoggerFactory.getLogger(ExampleProvider.class);

  @Override
  public void onSessionInitiated(ProviderSession session) {
    this.session = session;
  }

  @Override
  public Set<QName> getSupportedRpcs() {
    Set<QName> supportedRpcs = new HashSet<QName>();
    supportedRpcs.add(QNAME);
    return supportedRpcs;
  }

  @Override
  public RpcResult<CompositeNode> invokeRpc(final QName rpc, CompositeNode input) {
    boolean success = false;
    CompositeNode output = null;
    Collection<RpcError> errors = new ArrayList<>();

    // Only handle supported RPC calls
    if (getSupportedRpcs().contains(rpc))  {
      if (input == null) {
        errors.add(RpcErrors.getRpcError("app", "tag", "info", RpcError.ErrorSeverity.WARNING, "message:null input", RpcError.ErrorType.RPC, null));
      }
      else {
        if (isErroneousInput(input)) {
          errors.add(RpcErrors.getRpcError("app", "tag", "info", RpcError.ErrorSeverity.ERROR, "message:error", RpcError.ErrorType.RPC, null));
        }
        else {
          success = true;
          output = addSuccessNode(input);
        }
      }
    }
    return Rpcs.getRpcResult(success, output, errors);
  }

  // Examines input -- dives into CompositeNodes and finds any value equal to "error"
  private boolean isErroneousInput(CompositeNode input) {
    for (Node<?> n : input.getChildren()) {
      if (n instanceof CompositeNode) {
        if (isErroneousInput((CompositeNode)n)) {
          return true;
        }
      }
      else {  //SimpleNode
        if ((input.getChildren().get(0).getValue()).equals("error")) {
          return true;
        }
      }
    }
    return false;
  }
  
  // Adds a child SimpleNode containing the value "success" to the input CompositeNode
  private CompositeNode addSuccessNode(CompositeNode input) {
    List<Node<?>> list = new ArrayList<Node<?>>(input.getChildren());
    SimpleNodeTOImpl<String> simpleNode = new SimpleNodeTOImpl<String>(QNAME, input, "success");
    list.add(simpleNode);
    return new CompositeNodeTOImpl(QNAME, null, list);
  }

  @Override
  protected void startImpl(BundleContext context) {
    thisReg = context.registerService(ExampleProvider.class, this, new Hashtable<String, String>());
  }

  @Override
  protected void stopImpl(BundleContext context) {
    if (reg != null) {
      try {
        reg.close();
        thisReg.unregister();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public void announce(QName name) {
    _logger.debug("Announcing [{}]\n\n\n", name);
    reg = this.session.addRpcImplementation(name, this);
  }

}
