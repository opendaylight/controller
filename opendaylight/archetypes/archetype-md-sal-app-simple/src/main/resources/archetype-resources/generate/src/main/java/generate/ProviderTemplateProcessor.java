package ${package}.generate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class ProviderTemplateProcessor {
  private static String basePackage = "${package}";

  public static void processProviderTemplates(String appName, Set fieldKeys, VelocityEngine ve)  throws Exception{
    processProviderTemplate(appName, fieldKeys, ve);
    processProviderYang(appName, ve);
    processProviderSalImpl(appName, ve);
    processProviderSalYangModImpl(appName, ve);
    processProviderSalYangBinding(appName, ve);
    processProviderSalConfImpl(appName, ve);
    processProviderSalConfBuilder(appName, ve);
    processProviderSalDataBroker(appName, ve);
    processProviderSalRpc(appName, ve);
    processProviderSalBrokerBuilder(appName, ve);
    processProviderSalRpcBuilder(appName, ve);
    processProviderAbstractModule(appName, ve);
    processProviderAbstractModuleFactory(appName, ve);
    processProviderMXBean(appName, ve);
    processProviderModule(appName, ve);
    processProviderModuleFactory(appName, ve);
  }

  private static void processProviderTemplate(String appName, Set fieldKeys, VelocityEngine ve)  throws Exception{
    /*  next, get the Template  */
    Template template = ve.getTemplate( "provider/provider.vm" );
    /*  create a context and add data */
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    List<ProviderField> fields = new ArrayList<>();
    for(Object fieldKey : fieldKeys) {
      String name = (String)fieldKey;
      ProviderField field1 = new ProviderField(name, "set" +CodeGeneratorUtil.capitalizeFirstLetter(name));
      fields.add(field1);
    }
    String capitalAppName = CodeGeneratorUtil.capitalizeFirstLetter(appName);
    context.put("fields", fields);
    context.put("package", basePackage);
    String path = "provider/src/main/java/"+ basePackage.replaceAll("\\.", "/") + "/provider/"+capitalAppName+"Provider.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderYang(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/providerYang.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "provider/src/main/yang/"+ appName + "-provider-impl.yang";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalImpl(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/providerImpl.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);

    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/" + CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalYangModImpl(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/yangModuleInfoImpl.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/$YangModuleInfoImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalYangBinding(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/yangModelBindingProvider.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/$YangModelBindingProvider.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalConfImpl(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/config/providerImpl.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/modules/module/configuration/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderImpl.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalConfBuilder(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/config/providerImplBuilder.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/modules/module/configuration/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderImplBuilder.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalDataBroker(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/config/dataBroker.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/modules/module/configuration/"+ lowerApp +"/provider/impl/DataBroker.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalBrokerBuilder(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/config/dataBrokerBuilder.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/modules/module/configuration/"+ lowerApp +"/provider/impl/DataBrokerBuilder.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalRpc(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/config/rpcRegistry.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/modules/module/configuration/"+ lowerApp +"/provider/impl/RpcRegistry.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderSalRpcBuilder(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-sal/config/rpcRegistryBuilder.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String lowerApp = appName.toLowerCase();
    context.put("lowerApp", lowerApp);
    String path = "provider/src/main/yang-gen-sal/org/opendaylight/yang/gen/v1/urn/opendaylight/params/xml/ns/yang/controller/config/"
        + lowerApp + "/provider/impl/rev140523/modules/module/configuration/"+ lowerApp +"/provider/impl/RpcRegistryBuilder.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderAbstractModule(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-config/abstractProviderModule.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "provider/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
        + appName + "_provider/impl/Abstract"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderModule.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderAbstractModuleFactory(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-config/abstractProviderModuleFactory.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "provider/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
        + appName + "_provider/impl/Abstract"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderModuleFactory.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderMXBean(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/yang-gen-config/providerModuleMXBean.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "provider/src/main/yang-gen-config/org/opendaylight/controller/config/yang/config/"
        + appName + "_provider/impl/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderModuleMXBean.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderModule(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/providerModule.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    context.put("package", basePackage);
    String path = "provider/src/main/java/org/opendaylight/controller/config/yang/config/"
        + appName + "_provider/impl/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderModule.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }

  private static void processProviderModuleFactory(String appName, VelocityEngine ve)  throws Exception {
    Template template = ve.getTemplate( "provider/providerModuleFactory.vm" );
    VelocityContext context = CodeGeneratorUtil.createBasicVelocityContext(appName);
    String path = "provider/src/main/java/org/opendaylight/controller/config/yang/config/"
        + appName + "_provider/impl/"+ CodeGeneratorUtil.capitalizeFirstLetter(appName) +"ProviderModuleFactory.java";
    CodeGeneratorUtil.writeFile(path, context, template);
  }
}