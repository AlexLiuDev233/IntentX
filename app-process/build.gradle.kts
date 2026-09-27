plugins {
    alias(libs.plugins.intentx.library)
}

android {
    namespace = "io.github.wxxsfxyzm.app_process"

    buildFeatures {
        aidl = true
        buildConfig = true
    }
}

dependencies {
    compileOnly(project(":hidden-api"))
    compileOnly(libs.androidx.annotation)

    implementation(libs.commons.cli)
    implementation(libs.hiddenapibypass)
}
