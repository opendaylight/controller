<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
<@annotationsD object=annotations/>
<@typeDeclarationD object=typeDeclaration/>
{
    // attributes
    <@moduleFieldsD moduleFields=moduleFields/>
    //attributes end

    private static final ${loggerType} logger = ${loggerFactoryType}.getLogger(${typeDeclaration.name}.class);

    private final ${typeDeclaration.name} oldModule;
    private final ${instanceType} oldInstance;
    private ${instanceType} instance;
    private final ${dependencyResolverType} dependencyResolver;
    private final ${moduleNameType} identifier;
    <#if runtime=true>
    private ${registratorType} rootRuntimeBeanRegistratorWrapper;
    </#if>

    public ${typeDeclaration.name}(${moduleNameType} identifier, ${dependencyResolverType} dependencyResolver) {
        this.identifier = identifier;
        this.dependencyResolver = dependencyResolver;
        this.oldInstance = null;
        this.oldModule = null;
    }

    public ${typeDeclaration.name}(${moduleNameType} identifier, ${dependencyResolverType} dependencyResolver, ${typeDeclaration.name} oldModule, ${instanceType} oldInstance) {
        this.identifier = identifier;
        this.dependencyResolver = dependencyResolver;
        this.oldInstance = oldInstance;
        this.oldModule = oldModule;
    }

    // getters and setters exported into MXBean
    <@methodsD object=methods/>

    <#if runtime=true>
    public ${registratorType} getRootRuntimeBeanRegistratorWrapper(){
        return rootRuntimeBeanRegistratorWrapper;
    }

    @Override
    public void setRuntimeBeanRegistrator(${rootRuntimeRegistratorType} rootRuntimeRegistrator){
        this.rootRuntimeBeanRegistratorWrapper = new ${registratorType}(rootRuntimeRegistrator);
    }
    </#if>

    @Override
    public void validate(){
    <#list moduleFields as field>
        <#if field.dependent==true && field.dependency.mandatory==true>
        <#if field.type?starts_with("java.util.List")>
        for(javax.management.ObjectName dep : ${field.name}) {
            dependencyResolver.validateDependency(${field.dependency.sie.fullyQualifiedName}.class, dep, ${field.name}JmxAttribute);
        }
        <#else>
        dependencyResolver.validateDependency(${field.dependency.sie.fullyQualifiedName}.class, ${field.name}, ${field.name}JmxAttribute);
        </#if>
        </#if>
    </#list>
        customValidation();
    }

    protected void customValidation(){

    }

    // caches of resolved dependencies
    <#list moduleFields as field>
    <#if field.dependent==true>
        <#if field.type?starts_with("java.util.List")>
        private java.util.List<${field.dependency.sie.exportedOsgiClassName}> ${field.name}Dependency = new java.util.ArrayList<${field.dependency.sie.exportedOsgiClassName}>();
        protected final java.util.List<${field.dependency.sie.exportedOsgiClassName}> get${field.attributeName}Dependency(){
            return ${field.name}Dependency;
        }
        <#else>
        private ${field.dependency.sie.exportedOsgiClassName} ${field.name}Dependency;
        protected final ${field.dependency.sie.exportedOsgiClassName} get${field.attributeName}Dependency(){
            return ${field.name}Dependency;
        }
        </#if>
    </#if>
    </#list>

    // caches of resolved IdentityRefs
    <#list moduleFields as field>
    <#if field.identityRef==true>
        private ${field.identityClassType} ${field.identityClassName};
    </#if>
    </#list>

    @Override
    public final ${instanceType} getInstance(){
        if(instance==null) {

            <#list moduleFields as field>
                <#if field.dependent==true>
                    <#if field.dependency.mandatory==false>
                        if(${field.name}!=null) {
                    </#if>

                    <#if field.type?starts_with("java.util.List")>
            ${field.name}Dependency = new java.util.ArrayList<${field.dependency.sie.exportedOsgiClassName}>();
            for(javax.management.ObjectName dep : ${field.name}) {
                ${field.name}Dependency.add(dependencyResolver.resolveInstance(${field.dependency.sie.exportedOsgiClassName}.class, dep, ${field.name}JmxAttribute));
            }
                    <#else>
            ${field.name}Dependency = dependencyResolver.resolveInstance(${field.dependency.sie.exportedOsgiClassName}.class, ${field.name}, ${field.name}JmxAttribute);
                    </#if>

                    <#if field.dependency.mandatory==false>
                        }
                    </#if>
                </#if>

                <#if field.needsDepResolver==true>
            if(${field.name} != null) {
                    <#if field.type?starts_with("java.util.List")>
                for(${field.type?substring(field.type?index_of("<") + 1, field.type?index_of(">"))} candidate : ${field.name}) {
                    candidate.injectDependencyResolver(dependencyResolver);
                }
                    <#else>
                ${field.name}.injectDependencyResolver(dependencyResolver);
                    </#if>
            }
                </#if>

                <#if field.identityRef==true>
            if(${field.name} != null) {
                set${field.attributeName}(${field.name}.resolveIdentity(dependencyResolver, ${field.identityBaseClass}.class));
            }
                </#if>
            </#list>

            if(oldInstance!=null && canReuseInstance(oldModule)) {
                instance = reuseInstance(oldInstance);
            } else {
                if(oldInstance!=null) {
                    try {
                        oldInstance.close();
                    } catch(Exception e) {
                        logger.error("An error occurred while closing old instance " + oldInstance, e);
                    }
                }
                instance = createInstance();
            }
        }
        return instance;
    }

    @Override
    public ${moduleNameType} getIdentifier() {
        return identifier;
    }

    public boolean canReuseInstance(${typeDeclaration.name} oldModule){
        // allow reusing of old instance if no parameters was changed
        return isSame(oldModule);
    }

    public ${instanceType} reuseInstance(${instanceType} oldInstance){
        // implement if instance reuse should be supported. Override canReuseInstance to change the criteria.
        return oldInstance;
    }

    public abstract ${instanceType} createInstance();

    public boolean isSame(${typeDeclaration.name} other) {
        if (other == null) {
            throw new IllegalArgumentException("Parameter 'other' is null");
        }
        <#list moduleFields as field>
        <#if field.dependent==true && field.listOfDependencies == false>
        if (${field.name}Dependency != other.${field.name}Dependency) { // reference to dependency must be same
            return false;
        }
        <#elseif field.listOfDependencies>
        if (${field.name}Dependency.equals(other.${field.name}Dependency) == false) {
            return false;
        }
        for (int idx = 0; idx < ${field.name}Dependency.size(); idx++) {
            if (${field.name}Dependency.get(idx) != other.${field.name}Dependency.get(idx)) {
                return false;
            }
        }
        <#else>
        if (${field.name} == null) {
            if (other.${field.name} != null) {
                return false;
            }
        } else if
            <#if field.array == false>
                (${field.name}.equals(other.${field.name}) == false)
            <#else>
                (java.util.Arrays.equals(${field.name},other.${field.name}) == false)
            </#if>
                 {
            return false;
        }
        </#if>
        </#list>

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ${typeDeclaration.name} that = (${typeDeclaration.name}) o;

        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}
