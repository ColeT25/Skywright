import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform") version "2.4.21"
    id("com.diffplug.spotless") version "7.2.1"
}

@OptIn(ExperimentalDistributionDsl::class)
kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "skywright.js"
            }
            distribution {
                outputDirectory.set(layout.buildDirectory.dir("site"))
            }
        }
        nodejs {
            testTask { useMocha() }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-dom-api-compat:2.4.21")
            }
        }
        jsTest {
            dependencies { implementation(kotlin("test")) }
        }
    }
}

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.extensions.getByType<NodeJsRootExtension>().download = false
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
        progressiveMode.set(true)
    }
}

tasks.named<Kotlin2JsCompile>("compileTestDevelopmentExecutableKotlinJs") {
    compilerOptions.sourceMapEmbedSources.set(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.7.1")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.7.1")
    }
}
