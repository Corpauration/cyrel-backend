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
            val toBeImported = ArrayList<String>()
            val manyToManyMeta = HashMap<String, HashMap<String, String>>()
            properties.forEach {
                if (it.annotations.find { it.shortName.asString() == "ManyToMany" } == null)
                    propertiesMap.put(it.simpleName.asString(), it.type.toString() + if (it.type.resolve().isMarkedNullable) "?" else "")
                else {
                    val m = HashMap<String, String>()
                    m["type"] = it.type.element!!.typeArguments.first().type.toString()
                    m["import"] = it.type.element!!.typeArguments.first().type!!.resolve().declaration.packageName.asString()
                    m["table"] = it.annotations.find { it.shortName.asString() == "ManyToMany" }!!.arguments.find { it.name!!.asString() == "junction_table" }?.value as String
                    manyToManyMeta[it.simpleName.asString()] = m
                }

                if (it.type.resolve().declaration.packageName.asString() != "kotlin")
                    toBeImported.add("${it.type.resolve().declaration.packageName.asString()}.${it.type}")
            }
            val file = codeGenerator.createNewFile(Dependencies(true, classDeclaration.containingFile!!), packageName , className)
            file.appendText("""
                // [${propertiesMap.keys.joinToString(",")}]
                
            """.trimIndent())
            file.appendText(ClassBuilder(packageName, className)
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .add { input: ClassBuilder -> generateExtensionManyToMany(input, classDeclaration.simpleName.asString(), manyToManyMeta) }
                .add { input: ClassBuilder -> generateExtension(input, propertiesMap, classDeclaration.simpleName.asString(), toBeImported, manyToManyMeta) }
                .build())

            file.close()
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: KSAnnotation?) {

        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: KSAnnotation?) {

        }

        fun generateExtension(
            builder: ClassBuilder,
            properties: HashMap<String, String>,
            originalClass: String,
            toBeImported: ArrayList<String>,
            manyToManyMeta: HashMap<String, HashMap<String, String>>
        ): ClassBuilder {
            var b = builder
            var str = ""
            properties.forEach { name, clazz -> /*b = b.addImport(clazz);*/ str += "$name = row.getValue(\"$name\") as $clazz," }
            toBeImported.forEach { b = b.addImport(it) }
            return b
                .addImport("io.vertx.mutiny.pgclient.PgPool")
                .addImport("kotlinx.coroutines.launch")
                .addImport("kotlinx.coroutines.runBlocking")
                .addImport("kotlinx.coroutines.delay")
                .addImport("io.smallrye.mutiny.Uni")
                .addExtension("""
                    fun $originalClass.Companion.from(row: Row, client: PgPool): Uni<$originalClass> {
                        val o = $originalClass($str)
                        return Uni.combine().all().unis<$originalClass>(Uni.createFrom().item(o)${
                            kotlin.run {
                                var str = ""
                                manyToManyMeta.keys.forEachIndexed { i, it -> str += ", o.load_$it(client)" }
                                str
                            }
                        }).combinedWith {
                            ${
                                kotlin.run {
                                    var str = ""
                                    manyToManyMeta.keys.forEachIndexed { i, it -> str += "(it[0] as $originalClass).$it = (it[${i + 1}] as $originalClass).$it\n" }
                                    str
                                }
                            }
                            return@combinedWith it[0] as $originalClass
                        }
                    }
                """.trimIndent())
                /*.addExtension("""
                    fun $originalClass.load(client: PgPool): Uni<$originalClass> {
                        Uni
                    }
                """.trimIndent())*/
        }

        fun generateExtensionManyToMany(
            builder: ClassBuilder,
            originalClass: String,
            manyToManyMeta: HashMap<String, HashMap<String, String>>
        ): ClassBuilder {
            var b = builder
            manyToManyMeta.forEach { prop, metadata ->
                b = b
                    .addImport("io.vertx.mutiny.pgclient.PgPool")
                    .addImport("io.smallrye.mutiny.coroutines.awaitSuspending")
                    .addImport("io.smallrye.mutiny.Multi")
                    .addImport("io.smallrye.mutiny.Uni")
                    .addImport("io.vertx.mutiny.sqlclient.RowSet")
                    .addImport("io.vertx.mutiny.sqlclient.Row")
                    .addImport("java.util.function.Function")
                    .addImport("org.reactivestreams.Publisher")
                    .addImport("io.vertx.mutiny.sqlclient.Tuple")
                    .addImport("${metadata["import"]!!}.${metadata["type"]}")
                    .addImport("${metadata["import"]!!}.from")
                    .addExtension(
                    """
                    fun $originalClass.load_$prop(client: PgPool): Uni<$originalClass> {
                        val rowSet: Uni<RowSet<Row>> = client.preparedQuery("SELECT o.* FROM ${metadata["table"]!!.split("_")[1]} AS o JOIN ${metadata["table"]} AS oo ON oo.ref = o.id WHERE oo.id = $1").execute(Tuple.of(id))
                        return rowSet.onItem().transformToMulti(Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
                            Multi.createFrom().iterable(set)
                        }).flatMap { ${metadata["type"]}.from(it as Row, client).toMulti() }.collect().asList().onItem().transform { this.$prop = it.filterNotNull(); this }
                    }
                """.trimIndent())
                    .addExtension("""
                    suspend fun $originalClass.save_$prop(client: PgPool) {
                        client.withTransaction{
                            val queries = ArrayList<Uni<RowSet<Row>>>()
                            queries.add(it.preparedQuery("DELETE FROM ${metadata["table"]} WHERE id = $1").execute(Tuple.of(this.id)))
                            this.$prop.forEach{ v -> queries.add(it.preparedQuery("INSERT INTO ${metadata["table"]} (id, ref) VALUES ($1, $2)").execute(Tuple.of(this.id, v.id))) }
                            Uni.combine().all().unis<RowSet<Row>>(queries).discardItems()
                        }.awaitSuspending()
                    }
                    """.trimIndent())
            }


            return b

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