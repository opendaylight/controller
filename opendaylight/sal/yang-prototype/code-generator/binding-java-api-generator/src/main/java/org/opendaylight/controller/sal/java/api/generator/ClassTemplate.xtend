package org.opendaylight.controller.sal.java.api.generator

import java.util.List
import java.util.Map
import org.opendaylight.controller.binding.generator.util.TypeConstants
import org.opendaylight.controller.sal.binding.model.api.Constant
import org.opendaylight.controller.sal.binding.model.api.Enumeration
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject
import org.opendaylight.controller.sal.binding.model.api.Type

class ClassTemplate {
    
    val GeneratedTransferObject genTO
    val Map<String, String> imports
    val List<GeneratedProperty> fields
    val List<Enumeration> enums
    val List<Constant> consts
    
    new(GeneratedTransferObject genTO) {
        if (genTO == null) {
            throw new IllegalArgumentException("Generated transfer object reference cannot be NULL!")
        }
        
        this.genTO = genTO
        this.imports = GeneratorUtil.createImports(genTO)
        this.fields = genTO.properties
        this.enums = genTO.enumerations
        this.consts = genTO.constantDefinitions
    }
    
    def generate() {
        val body = generateBody(false)
        val pkgAndImports = generatePkgAndImports
        return pkgAndImports.toString + body.toString
    }
    
    def generateAsInnerClass() {
        return generateBody(true)
    }
    
    def private generateBody(boolean isInnerClass) '''
        «genTO.comment.generateComment»
        «generateClassDeclaration(isInnerClass)» {

            «generateEnums»
        
            «generateConstants»
        
            «generateFields»
        
            «generateConstructor»
        
            «FOR field : fields SEPARATOR "\n"»
                «field.generateGetter»
                «IF !field.readOnly»
                
                    «field.generateSetter»
                «ENDIF»
            «ENDFOR»
        
            «generateHashCode»
        
            «generateEquals»
        
            «generateToString»
        
        }
    '''
    
    def private generateComment(String comment) '''
        «IF comment != null && !comment.empty»
            /*
            «comment»
            */
        «ENDIF»
    '''
    
    def private generateClassDeclaration(boolean isInnerClass) '''
        public«
        IF (isInnerClass)»«
            " static final "»«
        ELSEIF (genTO.abstract)»«
            " abstract "»«
        ELSE»«
            " "»«
        ENDIF»class «genTO.name»«
        IF (genTO.extends != null)»«
            " extends "»«genTO.extends.resolveName»«
        ENDIF»«
        IF (!genTO.implements.empty)»«
            " implements "»«
            FOR type : genTO.implements SEPARATOR ", "»«
                type.resolveName»«
            ENDFOR»«
        ENDIF
    »'''
    
    def private generateEnums() '''
        «IF !enums.empty»
            «FOR e : enums SEPARATOR "\n"»
                «val enumTemplate = new EnumTemplate(e)»
                «enumTemplate.generateAsInnerClass»
            «ENDFOR»
        «ENDIF»
    '''
    
    def private generateConstants() '''
        «IF !consts.empty»
            «FOR c : consts»
                «IF c.name == TypeConstants.PATTERN_CONSTANT_NAME»
                    «val cValue = c.value»
                    «IF cValue instanceof List<?>»
                        «val cValues = cValue as List<?>»
                        private static final List<Pattern> «Constants.MEMBER_PATTERN_LIST» = new ArrayList<Pattern>();
                        public static final List<String> «TypeConstants.PATTERN_CONSTANT_NAME» = Arrays.asList(«
                        FOR v : cValues SEPARATOR ", "»«
                            IF v instanceof String»"«
                                v as String»"«
                            ENDIF»«
                        ENDFOR»);
                        
                        «generateStaticInicializationBlock»
                    «ENDIF»
                «ELSE»
                    public static final «c.type.resolveName» «c.name» = «c.value»;
                «ENDIF»
            «ENDFOR»
        «ENDIF»
    '''
    
    def private generateStaticInicializationBlock() '''
        static {
            for (String regEx : «TypeConstants.PATTERN_CONSTANT_NAME») {
                «Constants.MEMBER_PATTERN_LIST».add(Pattern.compile(regEx));
            }
        }
    '''
    def private generateFields() '''
        «IF !fields.empty»
            «FOR f : fields»
                private «f.returnType.resolveName» «f.name»;
            «ENDFOR»
        «ENDIF»
    '''
    
    def private generateConstructor() '''
        «val genTOTopParent = GeneratorUtil.getTopParrentTransportObject(genTO)»
        «val properties = GeneratorUtil.resolveReadOnlyPropertiesFromTO(genTO.properties)»
        «val propertiesAllParents = GeneratorUtil.getPropertiesOfAllParents(genTO)»
        «IF !genTO.unionType»
«««            create constructor for every parent property
            «IF genTOTopParent != genTO && genTOTopParent.unionType»
                «FOR parentProperty : propertiesAllParents SEPARATOR "\n"»
                    «val parentPropertyAndProperties = properties + #[parentProperty]»
                    «if (genTO.abstract) "protected" else "public"» «genTO.name»(«parentPropertyAndProperties.generateParameters») {
                        super(«#[parentProperty].generateParameterNames»);
                        «FOR property : properties»
                            this.«property.name» = «property.name»;
                        «ENDFOR»
                    }
                «ENDFOR»
«««            create one constructor
            «ELSE»
                «val propertiesAll = propertiesAllParents + properties»
                «if (genTO.abstract) "protected" else "public"» «genTO.name»(«propertiesAll.generateParameters») {
                    super(«propertiesAllParents.generateParameterNames()»);
                    «FOR property : properties»
                        this.«property.name» = «property.name»;
                    «ENDFOR»
                }
            «ENDIF»
«««        create constructor for every property
        «ELSE»
            «FOR property : properties SEPARATOR "\n"»
                «val propertyAndTopParentProperties = propertiesAllParents + #[property]»
                «if (genTO.abstract) "protected" else "public"» «genTO.name»(«propertyAndTopParentProperties.generateParameters») {
                    super(«propertiesAllParents.generateParameterNames()»);
                    this.«property.name» = «property.name»;
                }
            «ENDFOR»
        «ENDIF»
    '''
    
    def private generateGetter(GeneratedProperty field) '''
        public «field.returnType.resolveName» get«field.name.toFirstUpper»() {
            return «field.name»;
        }
    '''
    
    def private generateSetter(GeneratedProperty field) '''
        «val type = field.returnType.resolveName»
        public void set«field.name.toFirstUpper»(«type» «field.name») {
            this.«field.name» = «field.name»;
        }
    '''
    
    def private generateParameters(Iterable<GeneratedProperty> parameters) '''«
        IF !parameters.empty»«
            FOR parameter : parameters SEPARATOR ", "»«
                parameter.returnType.resolveName» «parameter.name»«
            ENDFOR»«
        ENDIF
    »'''
    
    def private generateParameterNames(Iterable<GeneratedProperty> parameters) '''«
        IF !parameters.empty»«
            FOR parameter : parameters SEPARATOR ", "»«
                parameter.name»«
            ENDFOR»«
        ENDIF
    »'''
    
    def private generateHashCode() '''
        «IF !genTO.hashCodeIdentifiers.empty»
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                «FOR property : genTO.hashCodeIdentifiers»
                    result = prime * result + ((«property.name» == null) ? 0 : «property.name».hashCode());
                «ENDFOR»
                return result;
            }
        «ENDIF»
    '''
    def private generateEquals() '''
        «IF !genTO.equalsIdentifiers.empty»
            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                «genTO.name» other = («genTO.name») obj;
                «FOR property : genTO.equalsIdentifiers»
                    «val fieldName = property.name»
                    if («fieldName» == null) {
                        if (other.«fieldName» != null) {
                            return false;
                        }
                    } else if(!«fieldName».equals(other.«fieldName»)) {
                        return false;
                    }
                «ENDFOR»
                return true;
            }
        «ENDIF»
    '''
    
    def private generateToString() '''
        «IF !genTO.toStringIdentifiers.empty»
            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                «val properties = genTO.toStringIdentifiers»
                builder.append("«genTO.name» [«properties.get(0).name»=");
                builder.append(«properties.get(0).name»);
                «FOR i : 1..<genTO.toStringIdentifiers.size»
                    builder.append(", «properties.get(i).name»=");
                    builder.append(«properties.get(i).name»);
                «ENDFOR»
                builder.append("]");
                return builder.toString();
            }
        «ENDIF»
    '''
    
    def private generatePkgAndImports() '''
        package «genTO.packageName»;
        
        
        «IF !imports.empty»
            «FOR entry : imports.entrySet»
                import «entry.value».«entry.key»;
            «ENDFOR»
        «ENDIF»
        
    '''
    
    def private resolveName(Type type) {
        GeneratorUtil.putTypeIntoImports(genTO, type, imports);
        GeneratorUtil.getExplicitType(genTO, type, imports)
    }
    
}