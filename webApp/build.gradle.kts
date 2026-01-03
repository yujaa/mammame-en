plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.5.11"
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }

    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.web.core)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.compose.html:html-svg:${extra["compose.version"] as String}")
            }
        }
    }
}
