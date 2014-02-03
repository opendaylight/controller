<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
<@typeDeclarationD object=typeDeclaration/> {

    public ${typeDeclaration.name}(${moduleNameType} identifier, ${dependencyResolverType} dependencyResolver,
            ${bundleContextType} bundleContext) {
        super(identifier, dependencyResolver, bundleContext);
    }

    public ${typeDeclaration.name}(${moduleNameType} identifier, ${dependencyResolverType} dependencyResolver,
            ${typeDeclaration.name} oldModule, ${instanceType} oldInstance, ${bundleContextType} bundleContext) {
        super(identifier, dependencyResolver, oldModule, oldInstance, bundleContext);
    }

    @Override
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public ${instanceType} createInstance() {
        //TODO:implement
        <@unimplementedExceptionD/>
    }
}
