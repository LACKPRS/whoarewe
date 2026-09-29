plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    tasks.register("recordRoborazziDebug") {
        description = "Stub task so -x recordRoborazziDebug does not fail"
    }
}
