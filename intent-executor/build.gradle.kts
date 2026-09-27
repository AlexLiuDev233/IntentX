// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

plugins {
    alias(libs.plugins.intentx.library)
}

android {
    namespace = "io.github.wxxsfxyzm.intentx.executor"
}

dependencies {
    implementation(project(":app-process"))
    compileOnly(project(":hidden-api"))
    implementation(libs.hiddenapibypass)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.timber)
    testImplementation(libs.kotlin.test.junit)
}
