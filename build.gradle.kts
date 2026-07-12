plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

if (providers.gradleProperty("isolatedBuildDir").isPresent) {
    subprojects {
        layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("isolated/${project.name}"))
    }
}
