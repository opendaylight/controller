package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

/**
 * 
 * CnSn = Composite node and Simple node data structure Class contains test of
 * serializing simple nodes data values according data types from YANG schema to
 * XML file
 * 
 */
public class CnSnInstanceIdentifierToXmlTest extends YangAndXmlAndDataSchemaLoader {
    @BeforeClass
    public static void initialization() throws URISyntaxException {
        dataLoad("/instanceidentifier/yang", 3, "instance-identifier-module", "cont");
//        ControllerContext controllerContext = ControllerContext.getInstance();
//        controllerContext.setSchemas(TestUtils.loadSchemaContext(modules));
//        controllerContext.findModuleNameByNamespace(new URI("instance:identifier:module"));
//        controllerContext.findModuleNameByNamespace(new URI("augment:module"));
//        controllerContext.findModuleNameByNamespace(new URI("augment:augment:module"));
    }

    @Test
    public void snAsYangInstanceIdentifier() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSnData = prepareCnStructForYangData( );
        String xmlOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSnData, modules, dataSchemaNode,
                StructuredDataToXmlProvider.INSTANCE);
        assertNotNull(xmlOutput);
//        System.out.println(xmlOutput);

    }

    private CompositeNode prepareCnStructForYangData() throws URISyntaxException {
        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("instance:identifier:module"), "cont");
        CompositeNodeWrapper cont1 = new CompositeNodeWrapper(new URI("augment:module"), "cont1");
        cont.addValue(cont1);
        SimpleNodeWrapper lf11 = new SimpleNodeWrapper(new URI("augment:augment:module"), "lf11", "/cont/cont1/lf12");
        SimpleNodeWrapper lf12 = new SimpleNodeWrapper(new URI("augment:augment:module"), "lf12", "lf12 value");
        cont1.addValue(lf11);
        cont1.addValue(lf12);
        cont.unwrap();
        return cont;
    }

    private IdentityValuesDTO prepareIdentityValueDTO() {
        IdentityValuesDTO result = new IdentityValuesDTO("instance:identifier:module", "cont", "");
        result.add("augment:module", "cont1", "");
        result.add("augment:augment:module", "lf12", "");
        return result;
    }
}
