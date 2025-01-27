// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.gradleJava.run

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.ExternalSystemRunTask
import org.jetbrains.kotlin.config.ExternalSystemTestRunTask
import org.jetbrains.kotlin.idea.facet.externalSystemTestRunTasks
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.plugins.gradle.execution.test.runner.*
import org.jetbrains.plugins.gradle.util.TasksToRun
import java.util.*
import java.util.function.Consumer

private typealias TaskFilter = (ExternalSystemTestRunTask) -> Boolean

class MultiplatformTestTasksChooser : TestTasksChooser() {
    companion object {
        fun createContext(context: DataContext, locationName: String?): DataContext {
            return contextWithLocationName(context, locationName)
        }
    }

    fun multiplatformChooseTasks(
        project: Project,
        dataContext: DataContext,
        elements: Iterable<PsiElement>,
        contextualSuffix: String? = null, // like "js, browser, HeadlessChrome85.0.4183, MacOSX10.14.6"
        handler: (List<Map<SourcePath, TestTasks>>) -> Unit
    ) {
        val consumer = Consumer<List<Map<SourcePath, TestTasks>>> { handler(it) }
        val testTasks = resolveTestTasks(elements, contextualFilter(contextualSuffix ))

        when {
            testTasks.isEmpty() -> super.chooseTestTasks(project, dataContext, elements, consumer)
            testTasks.size == 1 -> consumer.accept(testTasks.values.toList())
            else -> chooseTestTasks(project, dataContext, testTasks, consumer)
        }
    }

    private fun contextualFilter(contextualSuffix: String?): TaskFilter {
        val parts = contextualSuffix?.split(", ")
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: return { _ -> true }

        val targetName = parts[0]

        if (parts.size == 1) {
            return { it.targetName == targetName }
        }

        val taskPrefix = targetName + parts[1].capitalizeAsciiOnly()

        return { it.targetName == targetName && it.taskName.startsWith(taskPrefix) }
    }

    private fun resolveTestTasks(
        elements: Iterable<PsiElement>,
        taskFilter: TaskFilter
    ): Map<TestName, Map<SourcePath, TasksToRun>> {
        val tasks = mutableMapOf<TestName, MutableMap<SourcePath, TasksToRun>>()


        for (element in elements) {
            val module = element.module ?: continue
            val sourceFile = getSourceFile(element) ?: continue

            val groupedTasks = module.externalSystemTestRunTasks()
                .filter { taskFilter(it) }
                .groupBy { it.targetName }

            for ((group, tasksInGroup) in groupedTasks) {
                if (tasksInGroup.isEmpty()) {
                    continue
                } else if (tasksInGroup.size == 1) {
                    val task = tasksInGroup[0]
                    val presentableName = task.presentableName
                    val tasksMap = tasks.getOrPut(presentableName) { LinkedHashMap() }
                    tasksMap[sourceFile.path] = TasksToRun.Impl(presentableName, getTaskNames(task))
                } else {
                    for (task in tasksInGroup) {
                        val rawTaskName = ':' + task.taskName
                        val presentableName = if (group != null) "$group ($rawTaskName)" else rawTaskName
                        val tasksMap = tasks.getOrPut(presentableName) { LinkedHashMap() }
                        tasksMap[sourceFile.path] = TasksToRun.Impl(presentableName, getTaskNames(task))
                    }
                }
            }
        }

        return tasks
    }

    override fun chooseTestTasks(
        project: Project,
        context: DataContext,
        testTasks: Map<TestName, Map<SourcePath, TasksToRun>>,
        consumer: Consumer<List<Map<SourcePath, TestTasks>>>
    ) {
        if (isUnitTestMode()) {
            val result = mutableListOf<Map<SourcePath, TestTasks>>()

            for (tasks in testTasks.values) {
                result += tasks.mapValues { it.value }
            }

            consumer.accept(result)
            return
        }

        super.chooseTestTasks(project, context, testTasks, consumer)
    }

    private fun getTaskNames(task: ExternalSystemRunTask): List<String> {
        return listOf("clean" + task.taskName.capitalizeAsciiOnly(), task.taskName)
    }
}

private val ExternalSystemRunTask.presentableName: String
    get() = targetName ?: (":$taskName")
