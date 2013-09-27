<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
<@typeDeclarationD object=typeDeclaration/>
{

    public ${typeDeclaration.name}(${moduleNameType} name, ${dependencyResolverType} dependencyResolver) {
        super(name, dependencyResolver);
    }

    public ${typeDeclaration.name}(${moduleNameType} name, ${dependencyResolverType} dependencyResolver, ${typeDeclaration.name} oldModule, ${instanceType} oldInstance) {
        super(name, dependencyResolver, oldModule, oldInstance);
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
