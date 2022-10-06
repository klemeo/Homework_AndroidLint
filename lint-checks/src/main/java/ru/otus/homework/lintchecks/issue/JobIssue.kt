package ru.otus.homework.lintchecks.issue

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

class JobIssue : Detector(), Detector.UastScanner {

    companion object {
        const val MESSAGE =
            "Использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин"

        val ISSUE = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = MESSAGE,
            explanation = "...",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(JobIssue::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.inheritsFrom(
                context.evaluator.getTypeClass(node.receiverType),
                "kotlinx.coroutines.CoroutineScope",
                false
            )) {
            node.valueArguments.forEach {
                if (it !is ULambdaExpression) {
                    it.accept(
                        MethodVisitor(context, node, fix(), getApplicableMethodNames())
                    )
                }
            }
        }
    }
}