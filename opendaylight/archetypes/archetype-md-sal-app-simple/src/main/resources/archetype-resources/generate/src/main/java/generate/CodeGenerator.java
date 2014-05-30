#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )

package ${package}.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * This code generator use velocity templates to generate yang, java and jsp files,
 * which are used in model, provider and web projects respectively.
 * @author harmansingh
 *
 */
public class CodeGenerator {
  
  private static String basePackage = "${package}";
  
  /**
   * This method expects two arguments application name and fields for application,
   * which it will receive from command line, while generating the project.
   * If user does not specify those fields, a default value will be picked up.
   * arg[1] should be a valid JSON, otherwise, ParseException will be thrown. 
   * Second argument should be a valid string  
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {  
    //TODO : Do some preconditions check
    JSONParser parser = new JSONParser();
    Object obj = parser.parse(args[1]);
    JSONObject jsonObject = (JSONObject) obj;
    Set fieldKeys = jsonObject.keySet();
    /*  first, get and initialize an engine  */
    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.init();
    processModelYangTemplate(args[0], fieldKeys, ve, jsonObject);
    ProviderTemplateProcessor.processProviderTemplates(args[0], fieldKeys, ve);
    ConsumerTemplateProcessor.processConsumerTemplates(args[0], fieldKeys, ve);
    processWebViewTemplate(args[0], fieldKeys, ve);
    processInitialConfig(args[0], ve);
  }
  
  
  private static void processModelYangTemplate(String appName, Set fieldKeys, VelocityEngine ve, JSONObject jsonObject)
      throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "yang.vm" );
    /*  create a context and add data */
    VelocityContext context = new VelocityContext();
    context.put("app", appName);
    List<Field> fields = new ArrayList<>();
    for(Object fieldKey : fieldKeys) {
      Field field1 = new Field((String)fieldKey, (String)jsonObject.get(fieldKey));
      fields.add(field1);
    }
    context.put("fields", fields);
    /* now render the template into a File */
    String path = "model/src/main/yang/"+appName + ".yang";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processInitialConfig(String appName, VelocityEngine ve) throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "initialConfig.vm" );
    /*  create a context and add data */
    VelocityContext context = new VelocityContext();
    context.put("app", appName);
    /* now render the template into a File */
    String path = "configuration/initial/05-"+appName + "-sample.xml";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  
  private static void processWebViewTemplate(String appName, Set fieldKeys, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "view.vm" );
    /*  create a context and add data */
    VelocityContext context = new VelocityContext();
    context.put("fields", fieldKeys);
    context.put("app", appName);
    context.put("capitalApp", CodeGeneratorUtil.capitalizeFirstLetter(appName));
    String path = "web/src/main/resources/WEB-INF/pages/view.jsp";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

}
