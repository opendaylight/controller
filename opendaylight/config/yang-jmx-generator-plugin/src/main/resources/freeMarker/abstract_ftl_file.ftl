<@headerD header=header/>
package ${packageName};

<@javadocD object=javadoc/>
<@annotationsD object=annotations/>
<#-- class/interface -->
<@typeDeclarationD object=typeDeclaration/>
{
 
<@constructorsD object=constructors>
</@constructorsD>
<@fieldsD object=fields/>

<@methodsD object=methods/>
}
