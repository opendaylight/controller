package org.opendaylight.controller.sal.java.api.generator

import java.util.LinkedHashSet
import java.util.List
import java.util.Map
import java.util.Set
import org.opendaylight.controller.binding.generator.util.ReferencedTypeImpl
import org.opendaylight.controller.binding.generator.util.Types
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject
import org.opendaylight.controller.sal.binding.model.api.GeneratedType
import org.opendaylight.controller.sal.binding.model.api.MethodSignature
import org.opendaylight.controller.sal.binding.model.api.Type
import org.opendaylight.yangtools.yang.binding.Augmentable

class BuilderTemplate {

    val static GET_PREFIX = "get"
    val static JAVA_UTIL = "java.util"
    val static HASH_MAP = "HashMap"
    val static MAP = "Map"
    val static GET_AUGMENTATION_METHOD_NAME = "getAugmentation"
    val static BUILDER = 'Builder'
    val static IMPL = 'Impl'
    
    val GeneratedType genType
    val Map<String, String> imports
    var GeneratedProperty augmentField
    val Set<GeneratedProperty> fields
    
    new(GeneratedType genType) {
        if (genType == null) {
            throw new IllegalArgumentException("Generated type reference cannot be NULL!")
        }
        
        this.genType = genType
        this.imports = GeneratorUtil.createChildImports(genType)
        this.fields = createFieldsFromMethods(createMethods)
    }
    
    def private Set<MethodSignature> createMethods() {
        val Set<MethodSignature> methods = new LinkedHashSet
        methods.addAll(genType.methodDefinitions)
        storeMethodsOfImplementedIfcs(methods, genType.implements)
        return methods
    }
    
    def private void storeMethodsOfImplementedIfcs(Set<MethodSignature> methods, List<Type> implementedIfcs) {
        if (implementedIfcs == null || implementedIfcs.empty) {
            return
        }
        for (implementedIfc : implementedIfcs) {
            if ((implementedIfc instanceof GeneratedType && !(implementedIfc instanceof GeneratedTransferObject))) {
                val ifc = implementedIfc as GeneratedType
                methods.addAll(ifc.methodDefinitions)
                storeMethodsOfImplementedIfcs(methods, ifc.implements)
            } else if (implementedIfc.fullyQualifiedName == Augmentable.name) {
                for (m : Augmentable.methods) {
                    if (m.name == GET_AUGMENTATION_METHOD_NAME) {
                        addToImports(JAVA_UTIL, HASH_MAP)
                        addToImports(JAVA_UTIL, MAP)
                        val fullyQualifiedName = m.returnType.name
                        val pkg = fullyQualifiedName.package
                        val name = fullyQualifiedName.name
                        addToImports(pkg, name)
                        val tmpGenTO = new GeneratedTOBuilderImpl(pkg, name)
                        val type = new ReferencedTypeImpl(pkg, name)
                        val generic = new ReferencedTypeImpl(genType.packageName, genType.name)
                        val parametrizedReturnType = Types.parameterizedTypeFor(type, generic)
                        tmpGenTO.addMethod(m.name).setReturnType(parametrizedReturnType)
                        augmentField = tmpGenTO.toInstance.methodDefinitions.first.createFieldFromGetter
                    }
                }
            }
        }
    }
    
    def private void addToImports(String typePackageName,String typeName) {
        if (typePackageName.startsWith("java.lang") || typePackageName.isEmpty()) {
            return
        }
        if (!imports.containsKey(typeName)) {
            imports.put(typeName, typePackageName)
        }
    }
    
    def private <E> first(List<E> elements) {
        elements.get(0)
    }
    
    def private String getPackage(String fullyQualifiedName) {
        val lastDotIndex = fullyQualifiedName.lastIndexOf(Constants.DOT)
        return if (lastDotIndex == -1) "" else fullyQualifiedName.substring(0, lastDotIndex)
    }

    def private String getName(String fullyQualifiedName) {
        val lastDotIndex = fullyQualifiedName.lastIndexOf(Constants.DOT)
        return if (lastDotIndex == -1) fullyQualifiedName else fullyQualifiedName.substring(lastDotIndex + 1)
    }
    
    def private createFieldsFromMethods(Set<MethodSignature> methods) {
        val Set<GeneratedProperty> result = new LinkedHashSet

        if (methods == null || methods.isEmpty()) {
            return result
        }

        for (m : methods) {
            val createdField = m.createFieldFromGetter
            if (createdField != null) {
                result.add(createdField)
            }
        }
        return result
    }
    
    def private GeneratedProperty createFieldFromGetter(MethodSignature method) {
        if (method == null || method.name == null || method.name.empty || method.returnType == null) {
            throw new IllegalArgumentException("Method, method name, method return type reference cannot be NULL or empty!")
        }
        if (method.name.startsWith(GET_PREFIX)) {
            val fieldName = method.getName().substring(GET_PREFIX.length()).toFirstLower
            val tmpGenTO = new GeneratedTOBuilderImpl("foo", "foo")
            tmpGenTO.addProperty(fieldName).setReturnType(method.returnType)
            return tmpGenTO.toInstance.properties.first
        }
    }

    def generate() {
        val body = generateBody
        val pkgAndImports = generatePkgAndImports
        return pkgAndImports.toString + body.toString
    }
    
    def private generateBody() '''
        public class «genType.name»«BUILDER» {
        
            «generateFields»

            «generateSetters»

            public «genType.name» build() {
                return new «genType.name»«IMPL»();
            }

            private class «genType.name»«IMPL» implements «genType.name» {

                «generateFields»

                «generateConstructor»

                «generateGetters»

            }

        }
    '''

    def private generateFields() '''
        «IF !fields.empty»
            «FOR f : fields»
                private «f.returnType.resolveName» «f.name»;
            «ENDFOR»
        «ENDIF»
        «IF augmentField != null»
            private Map<Class<? extends «augmentField.returnType.resolveName»>, «augmentField.returnType.resolveName»> «augmentField.name» = new HashMap<>();
        «ENDIF»
    '''

    def private generateSetters() '''
        «FOR field : fields SEPARATOR '\n'»
            public «genType.name»«BUILDER» set«field.name.toFirstUpper»(«field.returnType.resolveName» «field.name») {
                this.«field.name» = «field.name»;
                return this;
            }
        «ENDFOR»
        «IF augmentField != null»
            
            public «genType.name»«BUILDER» add«augmentField.name.toFirstUpper»(Class<? extends «augmentField.returnType.resolveName»> augmentationType, «augmentField.returnType.resolveName» augmentation) {
                this.«augmentField.name».put(augmentationType, augmentation);
                return this;
            }
        «ENDIF»
    '''
    
    def private generateConstructor() '''
        private «genType.name»«IMPL»() {
            «IF !fields.empty»
                «FOR field : fields»
                    this.«field.name» = «genType.name»«BUILDER».this.«field.name»;
                «ENDFOR»
            «ENDIF»
            «IF augmentField != null»
                this.«augmentField.name».putAll(«genType.name»«BUILDER».this.«augmentField.name»);
            «ENDIF»
        }
    '''
    
    def private generateGetters() '''
        «IF !fields.empty»
            «FOR field : fields SEPARATOR '\n'»
                @Override
                public «field.returnType.resolveName» get«field.name.toFirstUpper»() {
                    return «field.name»;
                }
            «ENDFOR»
        «ENDIF»
        «IF augmentField != null»

            @SuppressWarnings("unchecked")
            @Override
            public <E extends «augmentField.returnType.resolveName»> E get«augmentField.name.toFirstUpper»(Class<E> augmentationType) {
                if (augmentationType == null) {
                    throw new IllegalArgumentException("Augmentation Type reference cannot be NULL!");
                }
                return (E) «augmentField.name».get(augmentationType);
            }
        «ENDIF»
    '''    
    
    def private generatePkgAndImports() '''
        package «genType.packageName»;
        
        
        «IF !imports.empty»
            «FOR entry : imports.entrySet»
                import «entry.value».«entry.key»;
            «ENDFOR»
        «ENDIF»
        
    '''
    
    def private resolveName(Type type) {
        GeneratorUtil.putTypeIntoImports(genType, type, imports);
        GeneratorUtil.getExplicitType(genType, type, imports)
    }
    
}

