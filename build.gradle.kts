plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "com.whisper3zzz.plugin"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        // === 默认配置：启动 IntelliJ IDEA Community (测试 Java/Kotlin) ===
        //create("IC", "2025.1.4.1")

        // === 测试 C++ 支持：启动 CLion ===
        // 请注释掉上面的 create("IC", ...) 并取消下面这行的注释
        // create("CL", "2024.3")

        // === 测试 C# 支持：启动 Rider ===
        // 请注释掉上面的 create("IC", ...) 并取消下面这行的注释
        create("RD", "2025.3")

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {{{{{{{{{{}}}}}}}}}
            sinceBuild = "251"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
