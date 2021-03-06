<@pp.dropOutputFile />
<#list dalgen.dos as DO>
<#if DO.fieldses?size gt 0>
<@pp.changeOutputFile name = "/main/java/${DO.classPath}/${DO.className}.java" />
package ${DO.packageName};

<#list DO.importLists as import>
<#if !import?ends_with("${DO.className}")>
import ${import};
</#if>
</#list>

/**
 * The table ${DO.desc!}
 */
public class ${DO.className}{

    <#list DO.fieldses as fields>
    /**
     * ${fields.name!} ${fields.desc!}.
     */
    private ${fields.javaType} ${fields.name};
    </#list>
    <#list DO.fieldses as fields>

    /**
     * Set ${fields.name!} ${fields.desc!}.
     */
    public void set${fields.name?cap_first}(${fields.javaType} ${fields.name}){
        this.${fields.name} = ${fields.name};
    }

    /**
     * Get ${fields.name!} ${fields.desc!}.
     *
     * @return the string
     */
    public ${fields.javaType} <#if fields.javaType=="boolean">is<#else>get</#if>${fields.name?cap_first}(){
    <#if fields.javaType=="Money">
        if(${fields.name}==null){
            return new Money();
        }
        return ${fields.name};
    <#else>
        return ${fields.name};
    </#if>
    }
    </#list>
}
</#if>
</#list>
