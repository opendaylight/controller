<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
<@typeDeclarationD object=typeDeclaration/>
{

    public static final java.lang.String NAME = "${globallyUniqueName}";
    private static final java.util.Set<Class<? extends ${abstractServiceInterfaceType}>> serviceIfcs = new java.util.HashSet<Class<? extends ${abstractServiceInterfaceType}>>();
    <#if providedServices??>
    static {
        <#list providedServices as refId>
        serviceIfcs.add(${refId});
        </#list>
    }
    </#if>

    @Override
    public final boolean isModuleImplementingServiceInterface(Class<? extends ${abstractServiceInterfaceType}> serviceInterface) {
        for (Class<?> ifc: serviceIfcs) {
            if (serviceInterface.isAssignableFrom(ifc)){
                return true;
            }
        }
        return false;
    }

    @Override
    public ${moduleType} createModule(String instanceName, ${dependencyResolverType} dependencyResolver, ${bundleContextType} bundleContext) {
        return instantiateModule(instanceName, dependencyResolver, bundleContext);
    }

    @Override
    public ${moduleType} createModule(String instanceName, ${dependencyResolverType} dependencyResolver, ${dynamicMBeanWithInstanceType} old, ${bundleContextType} bundleContext) throws Exception {
        ${moduleInstanceType} oldModule = null;
        try {
            oldModule = (${moduleInstanceType}) old.getModule();
        } catch(Exception e) {
            return handleChangedClass(old);
        }
        ${moduleInstanceType} module = instantiateModule(instanceName, dependencyResolver, oldModule, old.getInstance(), bundleContext);

        <#list fields as attr>
        module.set${attr.name}(oldModule.get${attr.name}());
        </#list>

        return module;
    }

    public ${moduleInstanceType} instantiateModule(String instanceName, ${dependencyResolverType} dependencyResolver, ${moduleInstanceType} oldModule, ${instanceType} oldInstance, ${bundleContextType} bundleContext) {
        return new ${moduleInstanceType}(new ${moduleNameType}(NAME, instanceName), dependencyResolver, oldModule, oldInstance);
    }

    public ${moduleInstanceType} instantiateModule(String instanceName, ${dependencyResolverType} dependencyResolver, ${bundleContextType} bundleContext) {
        return new ${moduleInstanceType}(new ${moduleNameType}(NAME, instanceName), dependencyResolver);
    }

    @Override
    public final String getImplementationName() {
        return NAME;
    }


    public ${moduleInstanceType} handleChangedClass(${dynamicMBeanWithInstanceType} old) throws Exception {
        throw new UnsupportedOperationException("Class reloading is not supported");
    }

    @Override
    public java.util.Set<${moduleInstanceType}> getDefaultModules(org.opendaylight.controller.config.api.DependencyResolverFactory dependencyResolverFactory, ${bundleContextType} bundleContext) {
        return new java.util.HashSet<${moduleInstanceType}>();
    }

}
