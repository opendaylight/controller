package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Preconditions;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.md.sal.rest.schema.SchemaExportContext;
import org.opendaylight.controller.md.sal.rest.schema.SchemaRetrievalService;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public class RestconfCompositeWrapper implements RestconfService, SchemaRetrievalService {

    private final RestconfService restconf;
    private final SchemaRetrievalService schema;

    public RestconfCompositeWrapper(final RestconfService restconf, final SchemaRetrievalService schema) {
        this.restconf = Preconditions.checkNotNull(restconf);
        this.schema = Preconditions.checkNotNull(schema);
    }

    @Override
    public Object getRoot() {
        return restconf.getRoot();
    }

    @Override
    public StructuredData getModules(final UriInfo uriInfo) {
        return restconf.getModules(uriInfo);
    }

    @Override
    public StructuredData getModules(final String identifier, final UriInfo uriInfo) {
        return restconf.getModules(identifier, uriInfo);
    }

    @Override
    public StructuredData getModule(final String identifier, final UriInfo uriInfo) {
        return restconf.getModule(identifier, uriInfo);
    }

    @Override
    public StructuredData getOperations(final UriInfo uriInfo) {
        return restconf.getOperations(uriInfo);
    }

    @Override
    public StructuredData getOperations(final String identifier, final UriInfo uriInfo) {
        return restconf.getOperations(identifier, uriInfo);
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final CompositeNode payload, final UriInfo uriInfo) {
        return restconf.invokeRpc(identifier, payload, uriInfo);
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        return restconf.invokeRpc(identifier, noPayload, uriInfo);
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        return restconf.readConfigurationData(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo uriInfo) {
        return restconf.readOperationalData(identifier, uriInfo);
    }

    @Override
    public Response updateConfigurationData(final String identifier, final Node<?> payload) {
        return restconf.updateConfigurationData(identifier, payload);
    }

    @Override
    public Response createConfigurationData(final String identifier, final Node<?> payload) {
        return restconf.createConfigurationData(identifier, payload);
    }

    @Override
    public Response createConfigurationData(final Node<?> payload) {
        return restconf.createConfigurationData(payload);
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        return restconf.deleteConfigurationData(identifier);
    }

    @Override
    public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
        return restconf.subscribeToStream(identifier, uriInfo);
    }

    @Override
    public StructuredData getAvailableStreams(final UriInfo uriInfo) {
        return restconf.getAvailableStreams(uriInfo);
    }

    @Override
    public SchemaExportContext getSchema(final String mountId) {
        return schema.getSchema(mountId);
    }
}
