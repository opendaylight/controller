package org.opendaylight.controller.sal.java.api.generator

import java.util.List
import java.util.Set

class BuilderTemplate {
	
	val static BUILDER = 'Builder'
	val static IMPL = 'Impl'
	
	def generate(BuilderClassDescriptor cd) '''
		package «cd.packageName»;
		«IF !cd.importsNames.empty»
			
			«FOR in : cd.importsNames»
				import «in»;
			«ENDFOR»
		«ENDIF»
		
		public class «cd.className»«BUILDER» {
		
			«fields(cd.fields, cd.augmentField)»
			
			«IF !cd.fields.empty»
				«FOR field : cd.fields SEPARATOR '\n'»
					public «cd.className»«BUILDER» set«field.name.toFirstUpper»(«field.type.name»«field.type.generics.print» «field.name») {
						this.«field.name» = «field.name»;
						return this;
					}
				«ENDFOR»
			«ENDIF»
			«IF cd.augmentField != null»
				
				public «cd.className»«BUILDER» add«cd.augmentField.name.toFirstUpper»(Class<? extends «cd.augmentField.type.name»«cd.augmentField.type.generics.print»> augmentationType, «cd.augmentField.type.name»«cd.augmentField.type.generics.print» augmentation) {
					this.«cd.augmentField.name».put(augmentationType, augmentation);
					return this;
				}
			«ENDIF»
			
			public «cd.className» build() {
				return new «cd.className»«IMPL»();
			}
			
			private class «cd.className»«IMPL» implements «cd.className» {
				
				«fields(cd.fields, cd.augmentField)»
				
				private «cd.className»«IMPL»() {
					«IF !cd.fields.empty»
						«FOR field : cd.fields»
							this.«field.name» = «cd.className»«BUILDER».this.«field.name»;
						«ENDFOR»
					«ENDIF»
					«IF cd.augmentField != null»
						this.«cd.augmentField.name».putAll(«cd.className»«BUILDER».this.«cd.augmentField.name»);
					«ENDIF»
				}
				
				«IF !cd.fields.empty»
					«FOR field : cd.fields SEPARATOR '\n'»
						@Override
						public «field.type.name»«field.type.generics.print» get«field.name.toFirstUpper»() {
							return «field.name»;
						}
					«ENDFOR»
				«ENDIF»
				«IF cd.augmentField != null»
					
					@Override
					public <E extends «cd.augmentField.type.name»«cd.augmentField.type.generics.print»> E get«cd.augmentField.name.toFirstUpper»(Class<E> augmentationType) {
						if (augmentationType == null) {
							throw new IllegalArgumentException("Augmentation Type reference cannot be NULL!");
						}
						return (E) «cd.augmentField.name».get(augmentationType);
					}
				«ENDIF»
				
			}
			
		}
	'''
	
	def private fields(Set<BuilderClassDescriptor.FieldDeclaration> fields, BuilderClassDescriptor.FieldDeclaration augmentField) '''
		«IF !fields.empty»
			«FOR field : fields»
				private «field.type.name»«field.type.generics.print» «field.name»;
			«ENDFOR»
		«ENDIF»
		«IF augmentField != null»
			private Map<Class<? extends «augmentField.type.name»«augmentField.type.generics.print»>, «augmentField.type.name»«augmentField.type.generics.print»> «augmentField.name» = new HashMap<>();
		«ENDIF»
	'''
	
	def private print(List<BuilderClassDescriptor.TypeDeclaration> generics) 
	'''«IF generics != null && !generics.empty»<«FOR generic : generics SEPARATOR ', '»«generic.name»«ENDFOR»>«ENDIF»'''
	
}