import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

plugins {
    kotlin("multiplatform") version "2.0.21"
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
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../app/src/main/java")
            kotlin.include(
                "com/skywright/game/GameCore.kt",
                "com/skywright/game/ScreenLayout.kt",
                "com/skywright/game/web/**",
            )
            resources.srcDir("src/main/resources")
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-dom-api-compat:2.0.21")
            }
        }
        jsTest {
            kotlin.srcDir("src/test/kotlin")
            kotlin.srcDir("../app/src/test/java")
            dependencies { implementation(kotlin("test")) }
        }
    }
}

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.extensions.getByType<NodeJsRootExtension>().download = false
}
