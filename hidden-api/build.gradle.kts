plugins {
    // Applied custom convention plugin instead of raw library plugin
    alias(libs.plugins.intentx.library)
}

android {
    namespace = "io.github.wxxsfxyzm.hidden_api"

    defaultConfig {
        minSdk = BuildConfig.MIN_SDK
    }
}

dependencies {
    // Add specific dependencies for hidden api stubs if needed
}
