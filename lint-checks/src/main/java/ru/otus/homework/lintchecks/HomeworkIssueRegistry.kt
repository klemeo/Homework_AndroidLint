package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.issue.ColorIssue
import ru.otus.homework.lintchecks.issue.GlobalScopeIssue
import ru.otus.homework.lintchecks.issue.JobIssue

class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(GlobalScopeIssue.ISSUE, JobIssue.ISSUE, ColorIssue.ISSUE)
}