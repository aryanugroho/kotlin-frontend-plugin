package org.jetbrains.kotlin.gradle.frontend.karma

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.frontend.*
import org.jetbrains.kotlin.gradle.frontend.util.*
import org.jetbrains.kotlin.gradle.frontend.webpack.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*

object KarmaLauncher : Launcher {
    override fun apply(packageManager: PackageManager, project: Project, startTask: Task, stopTask: Task) {
        val karma = project.extensions.create("karma", KarmaExtension::class.java)
        project.afterEvaluate {
            val compileTestKotlin = project.tasks.findByPath("compileTestKotlin2Js")

            if (compileTestKotlin != null && (compileTestKotlin as? Kotlin2JsCompile)?.kotlinOptions?.outputFile != null) {
                val karmaStart = project.tasks.create("karma-start", KarmaStartStopTask::class.java) {
                    it.start = true
                    it.onlyIf {
                        project.tasks.filterIsInstance<KotlinJsCompile>()
                                .filter { it.name.contains("test", ignoreCase = true) && it.kotlinOptions.outputFile != null }
                                .any { File(it.kotlinOptions.outputFile).exists() }
                    }
                }
                val karmaStop = project.tasks.create("karma-stop", KarmaStartStopTask::class.java) { it.start = false }

                project.tasks.getByName("test").dependsOn(karmaStart)

                karmaStart.dependsOn(compileTestKotlin)
                startTask.dependsOn(karmaStart)
                stopTask.dependsOn(karmaStop)

                packageManager.apply {
                    require("karma")

                    require("qunitjs", "1.23.1")
                    require("karma-qunit")

//            require("karma-junit-reporter")
                    require("karma-sourcemap-loader")

                    require("karma-phantomjs-launcher")
                    require("phantomjs-prebuilt")

                    var webPackRequireAdded = false
                    project.withTask(GenerateWebPackConfigTask::class) { task ->
                        if (!webPackRequireAdded) {
                            require("karma-webpack")
                            webPackRequireAdded = true
                        }

                        karma.enableWebPack = true
                        karmaStart.dependsOn(task)
                    }

                    if (project.extensions.getByType(KotlinFrontendExtension::class.java).sourceMaps) {
                        require("karma-sourcemap-loader")
                    }
                }
            }
        }

    }
}