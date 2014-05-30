#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )

package ${package}.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;


/**
 * This code generator use velocity templates to generate yang, java and jsp files,
 * which are used in model, provider and web projects respectively.
 * @author harmansingh
 *
 */
public class CodeGeneratorUtil {
  
  public static void writeFile(String path, VelocityContext context, Template template) throws Exception {
    File file = new File(path);
    File parent = file.getParentFile();
    if(!parent.exists()){
      parent.mkdirs();
    }
    // if file doesnt exists, then create it
    if (!file.exists()) {
      file.createNewFile();
    }
    FileWriter fw = new FileWriter(file.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);
    template.merge( context, bw );
    bw.close();
  }
  
  public static String capitalizeFirstLetter(String original){
    if(original.length() == 0)
        return original;
    return original.substring(0, 1).toUpperCase() + original.substring(1);
  }

  public static VelocityContext createBasicVelocityContext(String appName){
    VelocityContext context = new VelocityContext();
    context.put("capitalApp", capitalizeFirstLetter(appName));
    context.put("app", appName);
    return context;
  }

}
