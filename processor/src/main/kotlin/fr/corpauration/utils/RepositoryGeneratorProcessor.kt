package fr.corpauration.utils

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class RepositoryGeneratorProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {


    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("fr.corpauration.utils.RepositoryGenerator")
        logger.warn("RepositoryGeneratorProcessor is running")
        symbols.forEach {
            action: KSAnnotated -> logger.warn("annotation found with ${action::class}", action.parent)
        }
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSPropertyDeclaration && it.validate() }
            .forEach { it.accept(RepositoryGeneratorVisitor(), it.annotations.find {
                predicate: KSAnnotation -> predicate.shortName.asString() == "RepositoryGenerator"
            }) }
        return ret
    }

    inner class RepositoryGeneratorVisitor : KSVisitor<KSAnnotation?, Unit> {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KSAnnotation?) {
            logger.error("RepositoryGenerator should only be used on properties!", classDeclaration)
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: KSAnnotation?) {
            val parent = property.parentDeclaration as KSClassDeclaration
            val packageName = parent.containingFile!!.packageName.asString()
            val className = property.simpleName.asString().capitalize()
            val table = data?.arguments!!.find { predicate: KSValueArgument -> predicate.name!!.asString() == "table" }?.value
            val id = data.arguments.find { predicate: KSValueArgument -> predicate.name!!.asString() == "id" }?.value
            val entity = data.arguments.find { predicate: KSValueArgument -> predicate.name!!.asString() == "entity" }?.value
            val file = codeGenerator.createNewFile(Dependencies(true, property.containingFile!!), packageName , className)
            file.appendText(ClassBuilder(packageName, className)
                .addImport("javax.enterprise.context.ApplicationScoped")
                .addImport("javax.inject.Inject")
                .addImport("io.vertx.mutiny.pgclient.PgPool")
                .addImport("$packageName.from")
                .addImport("java.util.UUID")
                .set("table", table)
                .set("id", id)
                .set("entity", entity)
                .addClassAnotation("@ApplicationScoped")
                .addConstructorProprieties("client", "PgPool")
                /*.addField("""
                    @Inject
                    lateinit var client: PgPool
                """.trimIndent())*/
                .add { input: ClassBuilder -> generateGetAll(input) }
                .add { input: ClassBuilder -> generateGetIds(input) }
                .add { input: ClassBuilder -> generateFindById(input) }
                .add { input: ClassBuilder -> generateFindBy(input) }
                .add { input: ClassBuilder -> generateSave(input) }
                .add { input: ClassBuilder -> generateUpdate(input) }
                .add { input: ClassBuilder -> generateDelete(input) }
                .build())

            file.close()
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: KSAnnotation?) {

        }

        fun generateGetAll(builder: ClassBuilder): ClassBuilder {
            return builder
                .addImport("io.smallrye.mutiny.Multi")
                .addImport("io.smallrye.mutiny.Uni")
                .addImport("io.vertx.mutiny.sqlclient.RowSet")
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .addImport("java.util.function.Function")
                .addImport("org.reactivestreams.Publisher")
                .addFunction("""
                fun getAll(): Multi<${builder.get("entity")}> {
                    val rowSet: Uni<RowSet<Row>> = client.query("SELECT * FROM ${builder.get("table")}").execute()
                    return rowSet.onItem().transformToMulti(Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
                        Multi.createFrom().iterable(set)
                    }).flatMap { ${builder.get("entity")}.Companion.from(it as Row, client)!!.toMulti() }
                }
                """.trimIndent())
        }

        fun generateGetIds(builder: ClassBuilder): ClassBuilder {
            return builder
                .addImport("io.smallrye.mutiny.Multi")
                .addImport("io.smallrye.mutiny.Uni")
                .addImport("io.vertx.mutiny.sqlclient.RowSet")
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .addImport("java.util.function.Function")
                .addImport("org.reactivestreams.Publisher")
                .addFunction("""
                fun getIds(): Multi<${builder.get("id")}> {
                    val rowSet: Uni<RowSet<Row>> = client.query("SELECT id FROM ${builder.get("table")}").execute()
                    return rowSet.onItem().transformToMulti(Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
                        Multi.createFrom().iterable(set)
                    }).onItem().transform(Function<Any, ${builder.get("id")}> { row: Any ->
                        (row as Row).getValue("id") as ${builder.get("id")}
                    })
                }
                """.trimIndent())
        }

        fun generateFindById(builder: ClassBuilder): ClassBuilder {
            return builder.addImport("io.smallrye.mutiny.Multi")
                    .addImport("io.smallrye.mutiny.Uni")
                    .addImport("io.vertx.mutiny.sqlclient.RowSet")
                    .addImport("io.vertx.mutiny.sqlclient.RowIterator")
                    .addImport("io.vertx.mutiny.sqlclient.Row")
                    .addImport("io.vertx.mutiny.sqlclient.Tuple")
                    .addImport("java.util.function.Function")
                    .addImport("org.reactivestreams.Publisher")
                    .addFunction("""
                    fun findById(id: ${builder.get("id")}): Uni<${builder.get("entity")}> {
                        return client.preparedQuery("SELECT * FROM ${builder.get("table")} WHERE id = ${'$'}1").execute(Tuple.of(id)).onItem().transform(RowSet<Row>::iterator)
                        .flatMap{ if (it.hasNext()) ${builder.get("entity")}.from(it.next() as Row, client) else null }
                    }    
                    """.trimIndent())
        }

        fun generateFindBy(builder: ClassBuilder): ClassBuilder {
            return builder
                .addImport("io.smallrye.mutiny.Multi")
                .addImport("io.smallrye.mutiny.Uni")
                .addImport("io.vertx.mutiny.sqlclient.RowSet")
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .addImport("java.util.function.Function")
                .addImport("org.reactivestreams.Publisher")
                .addFunction("""
                    fun findBy(value: Any, field: String): Multi<${builder.get("entity")}> {
                        val rowSet: Uni<RowSet<Row>> = client.preparedQuery("SELECT * FROM ${builder.get("table")} WHERE ${"\$field"} = $1").execute(Tuple.of(value))
                        return rowSet.onItem().transformToMulti(Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
                            Multi.createFrom().iterable(set)
                        }).flatMap { ${builder.get("entity")}.Companion.from(it as Row, client)!!.toMulti() }
                    }    
                    """.trimIndent())
        }

        fun generateSave(builder: ClassBuilder): ClassBuilder {
            codeGenerator.generatedFile.forEach{ logger.warn(it.nameWithoutExtension) }
            val line = codeGenerator.generatedFile
                .filter { it.nameWithoutExtension == "${builder.get("entity")}Generated" }
                .first()
                .bufferedReader()
                .useLines {
                        lines: Sequence<String> ->
                    lines
                        .take(1)
                        .toList()
                        .first()
                }
            val l = Regex("\\[(.*)\\]").find(line)?.groupValues!![1].split(",").toTypedArray()
            return builder
                .addImport("io.smallrye.mutiny.Multi")
                .addImport("io.smallrye.mutiny.Uni")
                .addImport("io.vertx.mutiny.sqlclient.RowSet")
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .addImport("java.util.function.Function")
                .addImport("org.reactivestreams.Publisher")
                .addFunction("""
                    fun save(obj: ${builder.get("entity")}): Uni<RowSet<Row>> {
                        return client.preparedQuery("INSERT INTO ${builder.get("table")} (${l?.joinToString(", ")}) VALUES (${
                            kotlin.run {
                                val ll = (l.toMutableList())
                                ll.replaceAll{ "$${l.indexOf(it) + 1}" }
                                ll.joinToString(", ")
                            }
                        })").execute(Tuple.of(${
                            kotlin.run {
                                val ll = (l.toMutableList())
                                ll.replaceAll{ "obj.$it" }
                                ll.joinToString(", ")
                            }
                        }))
                    }    
                    """.trimIndent())
        }

        fun generateUpdate(builder: ClassBuilder): ClassBuilder {
            codeGenerator.generatedFile.forEach{ logger.warn(it.nameWithoutExtension) }
            val line = codeGenerator.generatedFile
                .filter { it.nameWithoutExtension == "${builder.get("entity")}Generated" }
                .first()
                .bufferedReader()
                .useLines {
                        lines: Sequence<String> ->
                    lines
                        .take(1)
                        .toList()
                        .first()
                }
            val l = Regex("\\[(.*)\\]")
                .find(line)?.groupValues!![1]
                .split(",")
                .toTypedArray()
                .filter { it != "id" }
            val ll = (l.toMutableList())
            ll.replaceAll{ "$it = $${l.indexOf(it) + 1}" }
            return builder
                .addImport("io.smallrye.mutiny.Multi")
                .addImport("io.smallrye.mutiny.Uni")
                .addImport("io.vertx.mutiny.sqlclient.RowSet")
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .addImport("java.util.function.Function")
                .addImport("org.reactivestreams.Publisher")
                .addFunction("""
                    fun update(obj: ${builder.get("entity")}): Uni<RowSet<Row>> {
                        return client.preparedQuery("UPDATE ${builder.get("table")} SET ${ll.joinToString(", ")} WHERE id = $${l.size + 1}").execute(Tuple.of(${
                    kotlin.run {
                        val lll = (l.toMutableList())
                        lll.replaceAll{ "obj.$it" }
                        lll.add("obj.id")
                        lll.joinToString(", ")
                    }
                }))
                    }    
                    """.trimIndent())
        }

        fun generateDelete(builder: ClassBuilder): ClassBuilder {
            return builder
                .addImport("io.smallrye.mutiny.Multi")
                .addImport("io.smallrye.mutiny.Uni")
                .addImport("io.vertx.mutiny.sqlclient.RowSet")
                .addImport("io.vertx.mutiny.sqlclient.Row")
                .addImport("java.util.function.Function")
                .addImport("org.reactivestreams.Publisher")
                .addFunction("""
                    fun delete(obj: ${builder.get("entity")}): Uni<RowSet<Row>> {
                        return client.preparedQuery("DELETE FROM ${builder.get("table")} WHERE id = $1").execute(Tuple.of(obj.id))
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

class RepositoryGeneratorProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return RepositoryGeneratorProcessor(environment.codeGenerator, environment.logger)
    }
}