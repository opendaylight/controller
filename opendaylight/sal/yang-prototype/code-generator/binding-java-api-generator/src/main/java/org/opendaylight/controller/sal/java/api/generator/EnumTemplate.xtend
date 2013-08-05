package org.opendaylight.controller.sal.java.api.generator

import org.opendaylight.controller.sal.binding.model.api.Enumeration

class EnumTemplate {
    
    val Enumeration enums
    
    new(Enumeration enums) {
        this.enums = enums
    }
    
    def generate() {
        val body = generateBody
        val pkg = generatePkg
        return pkg.toString + body.toString
    }
    
    def generateAsInnerClass() {
        return generateBody
    }
    
    def private generateBody() '''
        public enum «enums.name» {
        «FOR v : enums.values SEPARATOR ",\n"»
            «"    "»«v.name»(«v.value»)«
        ENDFOR»;
        
            int value;
        
            private «enums.name»(int value) {
                this.value = value;
            }
        }
    '''
    
    def private generatePkg() '''
        package «enums.packageName»;
        
        
    '''
    
}