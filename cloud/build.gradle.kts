import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

android {
    namespace = "com.hiresplayer.cloud"
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 26
        buildConfigField(
            "String",
            "YANDEX_CLIENT_ID",
            "\"${localProperties.getProperty("yandexClientId") ?: providers.gradleProperty("yandexClientId").getOrElse("YANDEX_CLIENT_ID_NOT_SET")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

afterEvaluate {
    tasks.named<Test>("testDebugUnitTest").configure {
        val unitTestClasses = files(
            layout.buildDirectory.dir("intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs"),
            layout.buildDirectory.dir("intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes")
        )
        testClassesDirs = unitTestClasses
        classpath += unitTestClasses
    }
}

tasks.register<Test>("logicTest") {
    description = "Runs JVM tests for pure cloud logic."
    group = "verification"
    dependsOn("compileDebugUnitTestJavaWithJavac", "compileDebugKotlin")
    val testClasses = file("$buildDir/intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes")
    val asciiRoot = file("${System.getProperty("java.io.tmpdir")}/hiresplayer-tests/cloud")
    val asciiTestClasses = file("$asciiRoot/test")
    val asciiMainClasses = file("$asciiRoot/main")
    testClassesDirs = files(testClasses)
    classpath = tasks.named<Test>("testDebugUnitTest").get().classpath + files(
        asciiTestClasses,
        asciiMainClasses
    )
    doFirst {
        delete(asciiRoot)
        copy {
            from(testClasses)
            into(asciiTestClasses)
        }
        copy {
            from(file("$buildDir/tmp/kotlin-classes/debug"))
            into(asciiMainClasses)
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
