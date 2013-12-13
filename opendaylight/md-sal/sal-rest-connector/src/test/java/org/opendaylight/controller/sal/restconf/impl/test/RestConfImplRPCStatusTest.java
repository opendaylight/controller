package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Set;
import java.util.logging.Level;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Charsets;

public class RestConfImplRPCStatusTest extends JerseyTest {

	private static ControllerContext controllerContext;
	private static BrokerFacade brokerFacade;
	private static RestconfImpl restconfImpl;
	private static final MediaType MEDIA_TYPE = new MediaType("application",
			"vnd.yang.data+xml");
	private static final MediaType MEDIA_TYPE_DRAFT02 = new MediaType(
			"application", "yang.data+xml");

	@BeforeClass
	public static void init() throws FileNotFoundException {
		Set<Module> allModules = TestUtils.loadModules(RestconfImplTest.class
				.getResource("/invoke-rpc-status").getPath());
		SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
		controllerContext = ControllerContext.getInstance();
		controllerContext.setSchemas(schemaContext);
		brokerFacade = mock(BrokerFacade.class);
		restconfImpl = RestconfImpl.getInstance();
		restconfImpl.setBroker(brokerFacade);
		restconfImpl.setControllerContext(controllerContext);
	}

	@Test
	public void testRPCStatusCodes() throws UnsupportedEncodingException,
			FileNotFoundException {

		String uri = createUri("/operations/", "invoke-rpc-status:testInput");

		InputStream xmlStream = RestconfImplTest.class
				.getResourceAsStream("/invoke-rpc-status/xml/data-rpc-input.xml");

		InputStream xmlStreamOutput = RestconfImplTest.class
				.getResourceAsStream("/invoke-rpc-status/xml/data-rpc-output.xml");
		CompositeNode outputCompositeNode = TestUtils
				.loadCompositeNode(xmlStreamOutput);

		String xml = TestUtils.getDocumentInPrintableForm(TestUtils
				.loadDocumentFrom(xmlStream));
		Entity<String> entity = Entity.entity(xml, MEDIA_TYPE_DRAFT02);

		when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class)))
				.thenReturn(
						new DummyRpcResult.Builder<CompositeNode>().isSuccessful(true)
								.result(null).build());
		Response response = target(uri).request(MEDIA_TYPE).post(entity);
		assertEquals(204, response.getStatus());

		when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class)))
				.thenReturn(
						new DummyRpcResult.Builder<CompositeNode>().isSuccessful(true)
								.result(outputCompositeNode).build());
		response = target(uri).request(MEDIA_TYPE).post(entity);
		assertEquals(200, response.getStatus());

		when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class)))
				.thenReturn(
						new DummyRpcResult.Builder<CompositeNode>().isSuccessful(false)
								.result(null).build());
		response = target(uri).request(MEDIA_TYPE).post(entity);
		assertEquals(500, response.getStatus());

		uri = createUri("/operations/", "invoke-rpc-status:testInputs");

		when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class)))
				.thenReturn(
						new DummyRpcResult.Builder<CompositeNode>().isSuccessful(true)
								.result(null).build());
		response = target(uri).request(MEDIA_TYPE).post(entity);
		assertEquals(404, response.getStatus());
	}

	@Override
	protected Application configure() {
		enable(TestProperties.LOG_TRAFFIC);
		enable(TestProperties.DUMP_ENTITY);
		enable(TestProperties.RECORD_LOG_LEVEL);
		set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());

		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig = resourceConfig.registerInstances(restconfImpl,
				StructuredDataToXmlProvider.INSTANCE,
				XmlToCompositeNodeProvider.INSTANCE);
		return resourceConfig;
	}

	private String createUri(String prefix, String encodedPart)
			throws UnsupportedEncodingException {
		return URI.create(
				prefix
						+ URLEncoder.encode(encodedPart,
								Charsets.US_ASCII.name()).toString())
				.toASCIIString();
	}
}
