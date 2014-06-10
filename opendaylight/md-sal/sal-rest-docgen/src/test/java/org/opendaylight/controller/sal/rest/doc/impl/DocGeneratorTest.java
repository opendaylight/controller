package org.opendaylight.controller.sal.rest.doc.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.base.Preconditions;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class DocGeneratorTest {

  private Set<Module> modules;
  private ObjectMapper mapper;

  public Set<Module> loadModules(String resourceDirectory) throws FileNotFoundException, URISyntaxException {

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
    return parser.parseYangModels(testFiles);
  }

  @Before
  public void before() throws Exception {
    modules = loadModules("/yang");
    mapper = new ObjectMapper();
    mapper.registerModule(new JsonOrgModule());
  }

  @After
  public void after() throws Exception {
  }

  /**
   * Method: getApiDeclaration(String module, String revision, UriInfo uriInfo)
   */
  @Test
  public void testGetModuleDoc() throws Exception {
    Preconditions.checkArgument(modules != null, "No modules found");

    for (Module m : modules){
      ApiDeclaration doc = ApiDocGenerator.getInstance().getSwaggerDocSpec(m, "http://localhost:8080/restconf");
      Assert.assertNotNull(doc);
    }
  }

}
