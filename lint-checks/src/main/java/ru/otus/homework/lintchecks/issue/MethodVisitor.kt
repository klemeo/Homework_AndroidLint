package ru.otus.homework.lintchecks.issue

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

class MethodVisitor(
    private val context: JavaContext,
    private val parentNode: UCallExpression,
    private val lintFix: LintFix.Builder,
    private val applicableCoroutineBuilderMethodNames: List<String>
) : AbstractUastVisitor() {

    override fun visitElement(node: UElement): Boolean {
        if (node is USimpleNameReferenceExpression) {

            if (context.evaluator.inheritsFrom(context.evaluator.getTypeClass(node.getExpressionType()), "kotlinx.coroutines.Job", false)) {
                val location = context.getLocation(node)

                context.report(
                    JobIssue.ISSUE,
                    node,
                    location,
                    JobIssue.MESSAGE,
                    getFix(
                        location,
                        context,
                        parentNode,
                        node
                    )
                )
            }
        }
        return false
    }

    private fun getFix(
        location: Location,
        context: JavaContext,
        parentNode: UCallExpression,
        node: USimpleNameReferenceExpression
    ): LintFix? {
        val parameterName = node.sourcePsi?.text.orEmpty()
        val parent = node.getParentOfType(UClass::class.java)?.javaPsi?.superClass

        return when {
            parameterName.contains("SupervisorJob") && parentNode.receiver?.sourcePsi?.text == "viewModelScope"
                    && parentNode.receiverType?.canonicalText == "kotlinx.coroutines.CoroutineScope"
                    && parent?.qualifiedName.equals("androidx.lifecycle.ViewModel")-> createSupervisorJobFix(location)
            node.getExpressionType()?.canonicalText == "kotlinx.coroutines.NonCancellable" &&
                    isCoroutineBuilderInvokeInOtherCoroutineBuilder(parentNode.uastParent) -> createNonCancellableFix(context.getLocation(parentNode.methodIdentifier))
            else -> null
        }
    }

    private tailrec fun isCoroutineBuilderInvokeInOtherCoroutineBuilder(
        uElement: UElement?
    ): Boolean {
        return when {
            uElement == null || uElement is UMethod -> false
            uElement is UCallExpression
                    && applicableCoroutineBuilderMethodNames.any { it == uElement.methodName } -> true
            else -> isCoroutineBuilderInvokeInOtherCoroutineBuilder(
                uElement.uastParent
            )
        }
    }

    private fun createSupervisorJobFix(location: Location): LintFix =
        lintFix
            .replace()
            .range(location)
            .all()
            .with("")
            .build()

    private fun createNonCancellableFix(location: Location): LintFix =
        lintFix
            .replace()
            .range(location)
            .all()
            .with("withContext")
            .build()
}