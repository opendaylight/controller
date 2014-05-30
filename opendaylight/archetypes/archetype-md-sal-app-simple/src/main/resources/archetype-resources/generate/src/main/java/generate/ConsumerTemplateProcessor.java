package ${package}.generate;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class ConsumerTemplateProcessor {

  private static String basePackage = "${package}";

  public static void processConsumerTemplates(String appName, Set fieldKeys, VelocityEngine ve)  throws Exception{
    processConsumerImpl(appName, ve);
    processConsumerService(appName, ve);
    processConsumerModule(appName, ve);
    processConsumerModuleFactory(appName, ve);
    processAbstractConsumerModule(appName, ve);
    processAbstractConsumerModuleFactory(appName, ve);
    processConsumerModuleMXBean(appName, ve);
    processConsumerServiceInterface(appName, ve);
    processAbstractConsumerService(appName, ve);
    processAbstractConsumerImpl(appName, ve);
    processYangModelBindingProvider(appName, ve);
    processYangModuleInfoImpl(appName, ve);
    processConsumerImplInterface(appName, ve);
    processConsumerImplBuilder(appName, ve);
    processRpcRegistry(appName, ve);
    processRpcRegistryBuilder(appName, ve);
    processConsumerYang(appName, ve);
  }

  private static void processConsumerImpl(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/consumerImpl.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "consumer/src/main/java/"+ basePackage.replaceAll("\\.", "/") + "/consumer/"
        + CodeGeneratorUtil.capitalizeFirstLetter(appName)+"ConsumerImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerService(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/consumerService.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "consumer/src/main/java/"+ basePackage.replaceAll("\\.", "/") + "/consumer/"
        + CodeGeneratorUtil.capitalizeFirstLetter(appName)+"ConsumerService.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerModule(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/consumerModule.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "consumer/src/main/java/org/opendaylight/controller/config/yang/config/"+
        appName +"_consumer/impl/" + CodeGeneratorUtil.capitalizeFirstLetter(appName)+"ConsumerModule.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerModuleFactory(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/consumerModuleFactory.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "consumer/src/main/java/org/opendaylight/controller/config/yang/config/"+
        appName +"_consumer/impl/" + CodeGeneratorUtil.capitalizeFirstLetter(appName)+"ConsumerModuleFactory.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerYang(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/consumerYang.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "consumer/src/main/yang/"+ appName + "-consumer-impl.yang";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerServiceInterface(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-config/consumerServiceInterface.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "consumer/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
    + appName+"_consumer/impl/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) + "ConsumerServiceServiceInterface.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processAbstractConsumerModule(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-config/abstractConsumerModule.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "consumer/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
        + appName+"_consumer/impl/Abstract"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) + "ConsumerModule.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processAbstractConsumerModuleFactory(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-config/abstractConsumerModuleFactory.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "consumer/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
        + appName+"_consumer/impl/Abstract"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) + "ConsumerModuleFactory.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerModuleMXBean(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-config/consumerModuleMXBean.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "consumer/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
        + appName+"_consumer/impl/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) + "ConsumerModuleMXBean.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processAbstractConsumerService(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/abstractConsumerService.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) + "ConsumerService.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processAbstractConsumerImpl(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/abstractConsumerImpl.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) + "ConsumerImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processYangModelBindingProvider(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/yangModelBindingProvider.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/$YangModelBindingProvider.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processYangModuleInfoImpl(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/yangModuleInfoImpl.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/$YangModuleInfoImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerImplInterface(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/config/consumerImpl.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/modules/module/configuration/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +
        "ConsumerImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processConsumerImplBuilder(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/config/consumerImplBuilder.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/modules/module/configuration/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +
        "ConsumerImplBuilder.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processRpcRegistry(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/config/rpcRegistry.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/modules/module/configuration/"+ lowerApp+
        "/consumer/impl/RpcRegistry.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processRpcRegistryBuilder(String appName, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "consumer/yang-gen-sal/config/rpcRegistryBuilder.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "consumer/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp+"/consumer/impl/rev140523/modules/module/configuration/"+ lowerApp+
        "/consumer/impl/RpcRegistryBuilder.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

}