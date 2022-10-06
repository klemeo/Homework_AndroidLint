package ru.otus.homework.lintchecks.issue

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

class GlobalScopeIssue : Detector(), SourceCodeScanner {

    companion object {
        private const val MESSAGE =
            "Корутины, запущенные на `kotlinx.coroutines.GlobalScope` нужно контролировать вне скоупа класса, в котором они созданы."

        val ISSUE = Issue.create(
            id = "GlobalScopeUsage",
            briefDescription = MESSAGE,
            explanation = """"
        Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти.
        """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(GlobalScopeIssue::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                if (node.receiverType?.canonicalText.equals("kotlinx.coroutines.GlobalScope") && node.methodName?.equals(
                        "launch"
                    ) == true
                ) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        MESSAGE,
                        createFix(context, node)
                    )
                }
            }
        }
    }

    private fun createFix(context: JavaContext, node: UCallExpression): LintFix? {
        val parent = node.getParentOfType(UClass::class.java)?.javaPsi?.superClass
        val dependencies = context.evaluator.dependencies?.compileDependencies //always null
        return if (parent?.qualifiedName.equals("androidx.lifecycle.ViewModel")
            && dependencies?.findLibrary("androidx.lifecycle:lifecycle-viewmodel-ktx")!=null) {
            fix()
                .replace()
                .range(context.getLocation(node.receiver))
                .with("viewModelScope")
                .build()
        } else if (parent?.qualifiedName.equals("androidx.fragment.app.Fragment")
            && dependencies?.findLibrary("androidx.lifecycle:lifecycle-runtime-ktx")!=null) {
            fix()
                .replace()
                .range(context.getLocation(node.receiver))
                .with("lifecycleScope")
                .build()
        } else null
    }
}