package org.opendaylight.controller.sal.rest.doc.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.base.Preconditions;

/**
 *
 */
public class DocGeneratorTest {

    private Map<File, Module> modules;
    private ObjectMapper mapper;

    public Map<File, Module> loadModules(String resourceDirectory) throws FileNotFoundException,
            URISyntaxException {

        URI resourceDirUri = getClass().getResource(resourceDirectory).toURI();
        final YangModelParser parser = new YangParserImpl();
        final File testDir = new File(resourceDirUri);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory.toString());
        }
        for (String fileName : fileList) {

            testFiles.add(new File(testDir, fileName));
        }
        return parser.parseYangModelsMapped(testFiles);
    }

    @Before
    public void before() throws Exception {
        modules = loadModules("/yang");
        mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: getApiDeclaration(String module, String revision, UriInfo
     * uriInfo)
     */
    @Test
    public void testGetModuleDoc() throws Exception {
        Preconditions.checkArgument(modules != null, "No modules found");

        for (Entry<File, Module> m : modules.entrySet()) {
            if (m.getKey().getAbsolutePath().endsWith("toaster_short.yang")) {
                ApiDeclaration doc = ApiDocGenerator.getInstance().getSwaggerDocSpec(m.getValue(),
                        "http://localhost:8080/restconf", "");
                validateToaster(doc);
                Assert.assertNotNull(doc);
            }
        }
    }

    private void validateToaster(ApiDeclaration doc) throws Exception {
        System.out.println( mapper.writeValueAsString(doc)  );
    }

}
