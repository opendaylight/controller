package org.opendaylight.controller.sal.rest.impl.test.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class TestInvokeMountPointRpc extends AbstractBodyReaderTest {

    private final JsonNormalizedNodeBodyReader jsonBodyReader;
    private final NormalizedNodeJsonBodyWriter jsonBodyWriter;
    private final RestconfService restconfService;
    private final BrokerFacade brokerFacade;
    private static SchemaContext schemaContext;
    private final DOMRpcService rpcService;

    public TestInvokeMountPointRpc() throws NoSuchFieldException, SecurityException {
        super();
        jsonBodyReader = new JsonNormalizedNodeBodyReader();
        jsonBodyWriter = new NormalizedNodeJsonBodyWriter();
        BrokerFacade.getInstance().setContext(mock(ConsumerSession.class));
        BrokerFacade.getInstance().setDomDataBroker(mock(DOMDataBroker.class));
        rpcService = mock(DOMRpcService.class);
        BrokerFacade.getInstance().setRpcService(rpcService);
        brokerFacade = BrokerFacade.getInstance();
        RestconfImpl.getInstance().setBroker(brokerFacade);
        RestconfImpl.getInstance().setControllerContext(controllerContext);
        restconfService = RestconfImpl.getInstance();
    }

    @Override
    protected MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_JSON, null);
    }

    /**
     * SchemaContext for RPCs initialization
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    @Before
    public void initialization() throws NoSuchFieldException, SecurityException {
        schemaContext = schemaContextLoader("/instanceidentifier/yang", schemaContext);
        schemaContext = schemaContextLoader("/modules", schemaContext);
        schemaContext = schemaContextLoader("/invoke-rpc", schemaContext);
        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContext);
        when(mountInstance.getService(DOMRpcService.class)).thenReturn(Optional.<DOMRpcService> of(rpcService));
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);
        controllerContext.setSchemas(schemaContext);
    }

    /**
     * Test RPC with Input and Output schemaNode definitions
     *
     * @throws Exception
     */
    @Test
    public void rpcModuleInputOutputTest() throws Exception {
        final String uri = "instance-identifier-module:cont/yang-ext:mount/invoke-rpc-module:rpc-test";
        final UriInfo uriInfo = mockBodyReader(uri, jsonBodyReader, true);

        final NormalizedNodeContext inputCx = parseRpcInput("/invoke-rpc/json/rpc-input.json");
        final SchemaPath schemaNodePath = inputCx.getInstanceIdentifierContext().getSchemaNode().getPath();
        final NormalizedNode<?, ?> inNormNode = inputCx.getData();
        final NormalizedNode<?, ?> outNormNode = parseRpcOutput("/invoke-rpc/json/rpc-output.json");

        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(outNormNode);
        final CheckedFuture<DOMRpcResult, DOMRpcException> rpcReturn = Futures.immediateCheckedFuture(rpcResult);

        when(rpcService.invokeRpc(schemaNodePath, inNormNode)).thenReturn(rpcReturn);
        final NormalizedNodeContext rpcResultCx = restconfService.invokeRpc(uri, inputCx, uriInfo);
        assertNotNull(rpcResultCx.getData());
        assertTrue(rpcResultCx.getInstanceIdentifierContext().getSchemaNode() instanceof RpcDefinition);
        assertEquals(inputCx.getInstanceIdentifierContext().getSchemaNode(), rpcResultCx.getInstanceIdentifierContext()
                .getSchemaNode());
        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(rpcResultCx, null, null, null, mediaType, null, output);
        assertTrue(output.toString().contains("lf-test"));
    }

    /**
     * Test RPC without Input but with Output schemaNode definitions
     *
     * @throws Exception
     */
    @Test
    public void rpcModuleNoInputOutputTest() throws Exception {
        final String uri = "instance-identifier-module:cont/yang-ext:mount/invoke-rpc-module:rpc-output-only-test";
        final UriInfo uriInfo = mockBodyReader(uri, jsonBodyReader, true);
        final NormalizedNodeContext inputCx = parseRpcInput(null);
        final SchemaPath schemaNodePath = inputCx.getInstanceIdentifierContext().getSchemaNode().getPath();
        final NormalizedNode<?, ?> inNormNode = inputCx.getData();
        final NormalizedNode<?, ?> outNormNode = parseRpcOutput("/invoke-rpc/json/rpc-output.json");

        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(outNormNode);
        final CheckedFuture<DOMRpcResult, DOMRpcException> rpcReturn = Futures.immediateCheckedFuture(rpcResult);
        when(rpcService.invokeRpc(schemaNodePath, inNormNode)).thenReturn(rpcReturn);

        final NormalizedNodeContext rpcResultCx = restconfService.invokeRpc(uri, inputCx, uriInfo);
        assertNotNull(rpcResultCx.getData());
        assertTrue(rpcResultCx.getInstanceIdentifierContext().getSchemaNode() instanceof RpcDefinition);
        assertEquals(inputCx.getInstanceIdentifierContext().getSchemaNode(), rpcResultCx.getInstanceIdentifierContext()
                .getSchemaNode());
        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(rpcResultCx, null, null, null, mediaType, null, output);
        assertTrue(output.toString().contains("lf-test"));
    }

    /**
     * Test RPC without Input but with Output schemaNode definitions post without ContentType
     *
     * @throws Exception
     */
    @Test
    public void rpcModuleNoInputOutputWithoutContentTypeTest() throws Exception {
        final String uri = "instance-identifier-module:cont/yang-ext:mount/invoke-rpc-module:rpc-output-only-test";
        final UriInfo uriInfo = mockBodyReader(uri, jsonBodyReader, true);
        final NormalizedNodeContext inputCx = parseRpcInput(null);
        final SchemaPath schemaNodePath = inputCx.getInstanceIdentifierContext().getSchemaNode().getPath();
        final NormalizedNode<?, ?> inNormNode = inputCx.getData();
        final NormalizedNode<?, ?> outNormNode = parseRpcOutput("/invoke-rpc/json/rpc-output.json");

        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(outNormNode);
        final CheckedFuture<DOMRpcResult, DOMRpcException> rpcReturn = Futures.immediateCheckedFuture(rpcResult);
        when(rpcService.invokeRpc(schemaNodePath, inNormNode)).thenReturn(rpcReturn);

        final NormalizedNodeContext rpcResultCx = restconfService.invokeRpc(uri, "", uriInfo);
        assertNotNull(rpcResultCx.getData());
        assertTrue(rpcResultCx.getInstanceIdentifierContext().getSchemaNode() instanceof RpcDefinition);
        assertEquals(inputCx.getInstanceIdentifierContext().getSchemaNode(), rpcResultCx.getInstanceIdentifierContext()
                .getSchemaNode());
        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(rpcResultCx, null, null, null, mediaType, null, output);
        assertTrue(output.toString().contains("lf-test"));
    }

    /**
     * Test RPC with Input but without Output schemaNode definitions
     *
     * @throws Exception
     */
    @Test
    public void rpcModuleInputNoOutputTest() throws Exception {
        final String uri = "instance-identifier-module:cont/yang-ext:mount/invoke-rpc-module:rpc-input-only-test";
        final UriInfo uriInfo = mockBodyReader(uri, jsonBodyReader, true);
        final NormalizedNodeContext inputCx = parseRpcInput("/invoke-rpc/json/rpc-input.json");
        final SchemaPath schemaNodePath = inputCx.getInstanceIdentifierContext().getSchemaNode().getPath();
        final NormalizedNode<?, ?> inNormNode = inputCx.getData();

        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(null, Collections.<RpcError> emptyList());
        final CheckedFuture<DOMRpcResult, DOMRpcException> rpcReturn = Futures.immediateCheckedFuture(rpcResult);

        when(rpcService.invokeRpc(schemaNodePath, inNormNode)).thenReturn(rpcReturn);
        final NormalizedNodeContext rpcResultCx = restconfService.invokeRpc(uri, inputCx, uriInfo);
        assertNull(rpcResultCx.getData());
        assertTrue(rpcResultCx.getInstanceIdentifierContext().getSchemaNode() instanceof RpcDefinition);
        assertEquals(inputCx.getInstanceIdentifierContext().getSchemaNode(), rpcResultCx.getInstanceIdentifierContext()
                .getSchemaNode());
        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(rpcResultCx, null, null, null, mediaType, null, output);
        assertTrue(Strings.isNullOrEmpty(output.toString()));
    }

    /**
     * Test RPC without Input and Output schemaNode definitions
     *
     * @throws Exception
     */
    @Test
    public void rpcModuleNoInputNoOutputTest() throws Exception {
        final String uri = "instance-identifier-module:cont/yang-ext:mount/invoke-rpc-module:rpc-noop";
        final UriInfo uriInfo = mockBodyReader(uri, jsonBodyReader, true);
        final NormalizedNodeContext inputCx = parseRpcInput(null);
        final SchemaPath schemaNodePath = inputCx.getInstanceIdentifierContext().getSchemaNode().getPath();
        final NormalizedNode<?, ?> inNormNode = inputCx.getData();

        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(null, Collections.<RpcError> emptyList());
        final CheckedFuture<DOMRpcResult, DOMRpcException> rpcReturn = Futures.immediateCheckedFuture(rpcResult);
        when(rpcService.invokeRpc(schemaNodePath, inNormNode)).thenReturn(rpcReturn);

        final NormalizedNodeContext rpcResultCx = restconfService.invokeRpc(uri, inputCx, uriInfo);
        assertNull(rpcResultCx.getData());
        assertTrue(rpcResultCx.getInstanceIdentifierContext().getSchemaNode() instanceof RpcDefinition);
        assertEquals(inputCx.getInstanceIdentifierContext().getSchemaNode(), rpcResultCx.getInstanceIdentifierContext()
                .getSchemaNode());
        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(rpcResultCx, null, null, null, mediaType, null, output);
        assertTrue(Strings.isNullOrEmpty(output.toString()));
    }

    /**
     * Test RPC without Input but with Output schemaNode definitions post without ContentType
     *
     * @throws Exception
     */
    @Test
    public void rpcModuleNoInputNoOutputWithoutContentTypeTest() throws Exception {
        final String uri = "invoke-rpc-module:rpc-noop";
        final UriInfo uriInfo = mockBodyReader(uri, jsonBodyReader, true);
        final NormalizedNodeContext inputCx = parseRpcInput(null);
        final SchemaPath schemaNodePath = inputCx.getInstanceIdentifierContext().getSchemaNode().getPath();
        final NormalizedNode<?, ?> inNormNode = inputCx.getData();

        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(null, Collections.<RpcError> emptyList());
        final CheckedFuture<DOMRpcResult, DOMRpcException> rpcReturn = Futures.immediateCheckedFuture(rpcResult);
        when(rpcService.invokeRpc(schemaNodePath, inNormNode)).thenReturn(rpcReturn);

        final NormalizedNodeContext rpcResultCx = restconfService.invokeRpc(uri, "", uriInfo);
        assertNull(rpcResultCx.getData());
        assertTrue(rpcResultCx.getInstanceIdentifierContext().getSchemaNode() instanceof RpcDefinition);
        assertEquals(inputCx.getInstanceIdentifierContext().getSchemaNode(), rpcResultCx.getInstanceIdentifierContext()
                .getSchemaNode());
        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(rpcResultCx, null, null, null, mediaType, null, output);
        assertTrue(Strings.isNullOrEmpty(output.toString()));
    }

    private NormalizedNodeContext parseRpcInput(final String filePath) throws WebApplicationException, IOException {
        final InputStream inStrem;
        if (Strings.isNullOrEmpty(filePath)) {
            inStrem = new ByteArrayInputStream("".getBytes());
        } else {
            inStrem = TestInvokeRpc.class.getResourceAsStream(filePath);
        }
        return jsonBodyReader.readFrom(null, null, null, mediaType, null, inStrem);
    }

    private NormalizedNode<?, ?> parseRpcOutput(final String filePath) throws WebApplicationException, IOException {
        final InputStream inStrem = TestInvokeRpc.class.getResourceAsStream(filePath);
        return jsonBodyReader.readFrom(null, null, null, mediaType, null, inStrem).getData();
    }
}
