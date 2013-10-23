<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
<@typeDeclarationD object=typeDeclaration/>
{

    public ${typeDeclaration.name}(${moduleNameType} identifier, ${dependencyResolverType} dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ${typeDeclaration.name}(${moduleNameType} identifier, ${dependencyResolverType} dependencyResolver, ${typeDeclaration.name} oldModule, ${instanceType} oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate(){
        super.validate();
        // Add custom validation for module attributes here.
    }

    @Override
    public ${instanceType} createInstance() {
        //TODO:implement
        <@unimplementedExceptionD/>
    }
}
