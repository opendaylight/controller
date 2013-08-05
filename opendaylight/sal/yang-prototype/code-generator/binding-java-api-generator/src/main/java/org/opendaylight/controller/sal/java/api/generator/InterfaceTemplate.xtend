package org.opendaylight.controller.sal.java.api.generator

import java.util.List
import java.util.Map
import org.opendaylight.controller.binding.generator.util.TypeConstants
import org.opendaylight.controller.sal.binding.model.api.Constant
import org.opendaylight.controller.sal.binding.model.api.Enumeration
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject
import org.opendaylight.controller.sal.binding.model.api.GeneratedType
import org.opendaylight.controller.sal.binding.model.api.MethodSignature
import org.opendaylight.controller.sal.binding.model.api.Type
import java.util.LinkedHashMap

class InterfaceTemplate {
    
    val GeneratedType genType
    val Map<String, String> imports
    val List<Constant> consts
    val List<MethodSignature> methods
    val List<Enumeration> enums
    val List<GeneratedType> enclosedGeneratedTypes
    
    new(GeneratedType genType) {
        if (genType == null) {
            throw new IllegalArgumentException("Generated type reference cannot be NULL!")
        }
        
        this.genType = genType
        imports = GeneratorUtil.createImports(genType)
        consts = genType.constantDefinitions
        methods = genType.methodDefinitions
        enums = genType.enumerations
        enclosedGeneratedTypes = genType.enclosedTypes
    }
    
    def generate() {
        val body = generateBody
        val pkgAndImports = generatePkgAndImports
        return pkgAndImports.toString + body.toString
    }
    
    def private generateBody() '''
        «genType.comment.generateComment»
        «generateIfcDeclaration» {
        
            «generateInnerClasses»
        
            «generateEnums»
        
            «generateConstants»
        
            «generateMethods»
        
        }
        
    '''
    
    def private generateComment(String comment) '''
        «IF comment != null && !comment.empty»
            /*
            «comment»
            */
        «ENDIF»
    '''
    
    def private generateIfcDeclaration() '''
        public interface «genType.name»«
        IF (!genType.implements.empty)»«
            " extends "»«
            FOR type : genType.implements SEPARATOR ", "»«
                type.resolveName»«
            ENDFOR»«
        ENDIF
    »'''
    
    def private generateInnerClasses() '''
        «IF !enclosedGeneratedTypes.empty»
            «FOR innerClass : enclosedGeneratedTypes SEPARATOR "\n"»
                «IF (innerClass instanceof GeneratedTransferObject)»
                    «val classTemplate = new ClassTemplate(innerClass as GeneratedTransferObject)»
                    «classTemplate.generateAsInnerClass»
                «ENDIF»
            «ENDFOR»
        «ENDIF»
    '''
    
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
                «IF c.name != TypeConstants.PATTERN_CONSTANT_NAME»
                    public static final «c.type.resolveName» «c.name» = «c.value»;
                «ENDIF»
            «ENDFOR»
        «ENDIF»
    '''
    
    def private generateMethods() '''
        «IF !methods.empty»
            «FOR m : methods SEPARATOR "\n"»
                «m.comment.generateComment»
                «m.returnType.resolveName» «m.name»(«m.parameters.generateParameters»);
            «ENDFOR»
        «ENDIF»
    '''
    
    def private generateParameters(List<MethodSignature.Parameter> parameters) '''«
        IF !parameters.empty»«
            FOR parameter : parameters SEPARATOR ", "»«
                parameter.type.resolveName» «parameter.name»«
            ENDFOR»«
        ENDIF
    »'''
    
    def private generatePkgAndImports() '''
        package «genType.packageName»;
        
        
        «IF !imports.empty»
            «FOR entry : resolveImports.entrySet»
                import «entry.value».«entry.key»;
            «ENDFOR»
        «ENDIF»
        
    '''
    
    def private Map<String, String> resolveImports() {
        val innerTypeImports = GeneratorUtil.createChildImports(genType)
        val Map<String, String> resolvedImports = new LinkedHashMap
        for (Map.Entry<String, String> entry : imports.entrySet() + innerTypeImports.entrySet) {
            val typeName = entry.getKey();
            val packageName = entry.getValue();
            if (packageName != genType.packageName && packageName != genType.fullyQualifiedName) {
                resolvedImports.put(typeName, packageName);
            }
        }
        return resolvedImports
    }
    
    def private resolveName(Type type) {
        GeneratorUtil.putTypeIntoImports(genType, type, imports);
        GeneratorUtil.getExplicitType(genType, type, imports)
    }
    
}   