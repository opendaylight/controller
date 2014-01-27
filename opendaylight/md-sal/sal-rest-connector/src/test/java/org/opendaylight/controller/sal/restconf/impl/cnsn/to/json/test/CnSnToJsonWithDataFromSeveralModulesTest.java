package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class CnSnToJsonWithDataFromSeveralModulesTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/xml-to-cnsn/data-of-several-modules/yang",2,"module1","cont_m1");
    }

    @Ignore
    @Test
    public void dataFromSeveralModulesToJsonTest() throws WebApplicationException, IOException, URISyntaxException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext(modules);
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(prepareCnSn(), modules, schemaContext,
                StructuredDataToJsonProvider.INSTANCE);

//         String output =
//         String.format("\"data\"   :   {\n" +
//                             "\t\"cont_m1\"   :  {\n" +
//                                 "\t\t\"lf1_m1\"   :  \"lf1 m1 value\"\n" +
//                             "\t}\n" +
//                             "\t\"cont_m2\"   :  {\n" +
//                                 "\t\t\"lf1_m2\"   :  \"lf1 m2 value\"\n" +
//                             "\t}\n" +
//         		"}");

        StringBuilder regex = new StringBuilder();
        regex.append("^");

        regex.append(".*\"data\"");
        regex.append(".*:");
        regex.append(".*\\{");
        
        regex.append(".*\"contB_m1\"");
        regex.append(".*:");
        regex.append(".*\\{");
        regex.append(".*\\}");
        
        regex.append(".*\"cont_m1\"");
        regex.append(".*:");
        regex.append(".*\\{");
        regex.append(".*\\}");

        regex.append(".*\"contB_m2\"");
        regex.append(".*:");
        regex.append(".*\\{");
        regex.append(".*\\}");
        
        regex.append(".*\"cont_m2\"");
        regex.append(".*:");
        regex.append(".*\\{");
        regex.append(".*\\}");
        
        regex.append(".*\\}");

        regex.append(".*");
        regex.append("$");

        Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        Matcher matcher = ptrn.matcher(output);

        assertTrue(matcher.find());

    }

    private CompositeNode prepareCnSn() throws URISyntaxException {
        CompositeNodeWrapper data = new CompositeNodeWrapper(new URI("urn:ietf:params:xml:ns:netconf:base:1.0"), "data");

        URI uriModule1 = new URI("module:one");
        CompositeNodeWrapper cont_m1 = new CompositeNodeWrapper(uriModule1, "cont_m1");
        SimpleNodeWrapper lf1_m1 = new SimpleNodeWrapper(uriModule1, "lf1_m1", "lf1 m1 value");
        cont_m1.addValue(lf1_m1);
        CompositeNodeWrapper contB_m1 = new CompositeNodeWrapper(uriModule1, "contB_m1");
        
        data.addValue(contB_m1);
        data.addValue(cont_m1);

        URI uriModule2 = new URI("module:two");
        CompositeNodeWrapper cont_m2 = new CompositeNodeWrapper(uriModule2, "cont_m2");
        SimpleNodeWrapper lf1_m2 = new SimpleNodeWrapper(uriModule2, "lf1_m2", "lf1 m2 value");
        cont_m2.addValue(lf1_m2);
        CompositeNodeWrapper contB_m2 = new CompositeNodeWrapper(uriModule2, "contB_m2");
        data.addValue(contB_m2);
        data.addValue(cont_m2);
        return data;
    }

}
