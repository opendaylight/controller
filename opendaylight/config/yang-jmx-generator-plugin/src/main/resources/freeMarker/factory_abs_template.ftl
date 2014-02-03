<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
@org.opendaylight.yangtools.yang.binding.annotations.ModuleQName(namespace="${mbe.getYangModuleQName().getNamespace().toString()}",revision="${mbe.getYangModuleQName().getFormattedRevision()}",name="${mbe.getYangModuleQName().getLocalName()}")
<@typeDeclarationD object=typeDeclaration/>
{

    public static final java.lang.String NAME = "${globallyUniqueName}";
    private static final java.util.Set<Class<? extends ${abstractServiceInterfaceType}>> serviceIfcs;
    <#if providedServices??>
    static {
        java.util.Set<Class<? extends ${abstractServiceInterfaceType}>> serviceIfcs2 = new java.util.HashSet<Class<? extends ${abstractServiceInterfaceType}>>();
        <#list providedServices as refId>
        serviceIfcs2.add(${refId});
        </#list>
        serviceIfcs = java.util.Collections.unmodifiableSet(serviceIfcs2);
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
    public java.util.Set<Class<? extends ${abstractServiceInterfaceType}>> getImplementedServiceIntefaces() {
        return serviceIfcs;
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
        // Workaround for backwards compatibility
        // Try to find new constructor with BundleContext
        try {
            java.lang.reflect.Constructor<${moduleInstanceType}> constructor = ${moduleInstanceType}.class.getConstructor(${moduleNameType}.class, ${dependencyResolverType}.class, ${moduleInstanceType}.class, ${instanceType}.class, ${bundleContextType}.class);
            try {
                return constructor.newInstance(new ${moduleNameType}(NAME, instanceName), dependencyResolver, oldModule, oldInstance, bundleContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException e) {
            // If new constructor is not present, try the old one
            try {
                java.lang.reflect.Constructor<${moduleInstanceType}> constructor = ${moduleInstanceType}.class.getConstructor(${moduleNameType}.class, ${dependencyResolverType}.class, ${moduleInstanceType}.class, ${instanceType}.class);
                return constructor.newInstance(new ${moduleNameType}(NAME, instanceName), dependencyResolver, oldModule, oldInstance);
            } catch (Exception eI) {
                eI.addSuppressed(e);
                throw new RuntimeException(eI);
            }
        }
    }

    public ${moduleInstanceType} instantiateModule(String instanceName, ${dependencyResolverType} dependencyResolver, ${bundleContextType} bundleContext) {
        // Workaround for backwards compatibility
        // Try to find new constructor with BundleContext
        try {
            java.lang.reflect.Constructor<${moduleInstanceType}> constructor = ${moduleInstanceType}.class.getConstructor(${moduleNameType}.class, ${dependencyResolverType}.class, ${bundleContextType}.class);
            try {
                return constructor.newInstance(new ${moduleNameType}(NAME, instanceName), dependencyResolver, bundleContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException e) {
            // If new constructor is not present, try the old one
            try {
                java.lang.reflect.Constructor<${moduleInstanceType}> constructor = ${moduleInstanceType}.class.getConstructor(${moduleNameType}.class, ${dependencyResolverType}.class);
                return constructor.newInstance(new ${moduleNameType}(NAME, instanceName), dependencyResolver);
            } catch (Exception eI) {
                eI.addSuppressed(e);
                throw new RuntimeException(eI);
            }
        }
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
