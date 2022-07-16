package fr.corpauration.utils

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate


class EntityProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {


    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("fr.corpauration.utils.Entity")
        logger.warn("EntityProcessor is running")
        symbols.forEach {
                action: KSAnnotated -> logger.warn("annotation found with ${action::class}", action.parent)
        }
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(EntityVisitor(), it.annotations.find {
                    predicate: KSAnnotation -> predicate.shortName.asString() == "Entity"
            }) }
        return ret
    }

    inner class EntityVisitor : KSVisitor<KSAnnotation?, Unit> {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KSAnnotation?) {
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = classDeclaration.simpleName.asString() + "Generated"
            val properties = classDeclaration.getDeclaredProperties()
            val propertiesMap: HashMap<String, String> = HashMap()
            properties.forEach {
                propertiesMap.put(it.simpleName.asString(), it.type.toString())
            }
            val file = codeGenerator.createNewFile(Dependencies(true, classDeclaration.containingFile!!), packageName , className)
            file.appendText("""
                // [${propertiesMap.keys.joinToString(",")}]
                
            """.trimIndent())
            file.appendText(ClassBuilder(packageName, className)
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .add { input: ClassBuilder -> generateExtension(input, propertiesMap, classDeclaration.simpleName.asString()) }
                .build())

            file.close()
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: KSAnnotation?) {

        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: KSAnnotation?) {

        }

        fun generateExtension(builder: ClassBuilder, properties: HashMap<String, String>, originalClass: String): ClassBuilder {
            var b = builder
            var str = ""
            properties.forEach { name, clazz -> /*b = b.addImport(clazz);*/ str += "$name = row.getValue(\"$name\") as $clazz," }
            return b
                .addExtension("""
                    fun $originalClass.Companion.from(row: Row): $originalClass {
                        return $originalClass($str)
                    }
                """.trimIndent())
        }

        override fun visitAnnotated(annotated: KSAnnotated, data: KSAnnotation?) {

        }

        override fun visitAnnotation(annotation: KSAnnotation, data: KSAnnotation?) {

        }

        override fun visitCallableReference(reference: KSCallableReference, data: KSAnnotation?) {

        }

        override fun visitClassifierReference(reference: KSClassifierReference, data: KSAnnotation?) {

        }

        override fun visitDeclaration(declaration: KSDeclaration, data: KSAnnotation?) {

        }

        override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: KSAnnotation?) {

        }

        override fun visitDynamicReference(reference: KSDynamicReference, data: KSAnnotation?) {

        }

        override fun visitFile(file: KSFile, data: KSAnnotation?) {

        }

        override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: KSAnnotation?) {

        }

        override fun visitNode(node: KSNode, data: KSAnnotation?) {

        }

        override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: KSAnnotation?) {

        }

        override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: KSAnnotation?) {

        }

        override fun visitPropertyGetter(getter: KSPropertyGetter, data: KSAnnotation?) {

        }

        override fun visitPropertySetter(setter: KSPropertySetter, data: KSAnnotation?) {

        }

        override fun visitReferenceElement(element: KSReferenceElement, data: KSAnnotation?) {

        }

        override fun visitTypeAlias(typeAlias: KSTypeAlias, data: KSAnnotation?) {

        }

        override fun visitTypeArgument(typeArgument: KSTypeArgument, data: KSAnnotation?) {

        }

        override fun visitTypeParameter(typeParameter: KSTypeParameter, data: KSAnnotation?) {

        }

        override fun visitTypeReference(typeReference: KSTypeReference, data: KSAnnotation?) {

        }

        override fun visitValueArgument(valueArgument: KSValueArgument, data: KSAnnotation?) {

        }

        override fun visitValueParameter(valueParameter: KSValueParameter, data: KSAnnotation?) {

        }
    }
}

class EntityProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return EntityProcessor(environment.codeGenerator, environment.logger)
    }
}