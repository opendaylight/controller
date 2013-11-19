package org.opendaylight.controller.sample.zeromq.provider;

import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connector.remoterpc.dto.CompositeNodeImpl;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

public class ExampleProvider extends AbstractProvider implements RpcImplementation {

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");
  private final QName QNAME_TWO = new QName(namespace, "heartbeat2");
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
    supportedRpcs.add(QNAME_TWO);
    return supportedRpcs;
  }

  @Override
  public RpcResult<CompositeNode> invokeRpc(final QName rpc, CompositeNode input) {

    CompositeNode successCompositeNode = new CompositeNodeImpl();


    boolean success = false;
    CompositeNode output = null;
    Collection<RpcError> errors = new ArrayList<>();

    // Only handle supported RPC calls
//    if (getSupportedRpcs().contains(rpc))  {
//      if (input == null) {
//        errors.add(RpcErrors.getRpcError("app", "tag", "info", RpcError.ErrorSeverity.ERROR, "message:null input", RpcError.ErrorType.RPC, null));
//      }
//      else if (!rpc.equals(input.getKey())) {
//        errors.add(RpcErrors.getRpcError("app", "tag", "info", RpcError.ErrorSeverity.WARNING, "message:key mismatch", RpcError.ErrorType.RPC, null));
//      }
//      else {
//        success = true;
//        output = successCompositeNode;
//      }
//    }
//    else {
//      // Invalid Service
//      // I don't think this code can ever get executed in the integration test, no way for the request to get here b/c no announce for invalid service
//      errors.add(RpcErrors.getRpcError("app", "tag", "info", RpcError.ErrorSeverity.ERROR, "message:unsupported RPC", RpcError.ErrorType.RPC, null));
//    }
    output = successCompositeNode;
    return Rpcs.getRpcResult(success, output, errors);
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
