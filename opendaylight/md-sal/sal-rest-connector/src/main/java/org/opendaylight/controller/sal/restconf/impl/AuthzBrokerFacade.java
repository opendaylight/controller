package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.aaa.api.AuthenticationService;
import org.opendaylight.aaa.api.Authentication;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

/**
 * @author lmukkama
 *         Date: 7/14/14
 */
public class AuthzBrokerFacade implements DataReader<InstanceIdentifier, CompositeNode> {

    private final static Logger LOG = LoggerFactory.getLogger(AuthzBrokerFacade.class);

    private final static AuthzBrokerFacade INSTANCE = new AuthzBrokerFacade();

    private volatile AuthenticationService authService;

    private volatile ConsumerSession context;

    private BrokerFacade broker;


    private AuthzBrokerFacade(){

    }

    public static AuthzBrokerFacade getInstance(){
        return AuthzBrokerFacade.INSTANCE;
    }

    public void setContext( final ConsumerSession context ) {
        this.context = context;
    }

    public void setAuthService(AuthenticationService authService){
        this.authService = authService;
    }

    public void setBroker(final BrokerFacade broker) {
        this.broker = broker;
    }

    private void checkPreconditions() {
        if( context == null || broker == null) {
            throw new RestconfDocumentedException( Response.Status.SERVICE_UNAVAILABLE );
        }
    }

    @Override
    public CompositeNode readConfigurationData( final InstanceIdentifier path ) {
        this.checkPreconditions();

        LOG.trace( "Read Configuration via Restconf Authz: {}", path );

        //Get the claim for the request

        //Authentication auth = authService.get();
        //Perform the Authz check with the claim to the Authz Service
        boolean authzRes = true;

        //Based on the result allow the access or return RestconfDocumentedException( Response.Status.UNAUTHORIZED )
        if(authzRes){
            return broker.readConfigurationData( path );
        }else{
            throw new RestconfDocumentedException( Response.Status.UNAUTHORIZED );
        }
    }

    @Override
    public CompositeNode readOperationalData( final InstanceIdentifier path ) {
        this.checkPreconditions();

        LOG.trace( "Read Operational via Restconf Authz: {}", path );

        //Get the claim for the request

        Authentication auth = this.authService.get();
        //Perform the Authz check with the claim to the Authz Service
        boolean authzRes = true;

        //Based on the result allow the access or return RestconfDocumentedException( Response.Status.UNAUTHORIZED )
        if(authzRes){
            return broker.readOperationalData( path );
        }else{
            throw new RestconfDocumentedException( Response.Status.UNAUTHORIZED );
        }
    }
}
