// Pinned to a known-good combo (Gradle 8.11 / AGP 8.7 / Kotlin 2.1) so the build
// is reproducible regardless of the machine's bleeding-edge JDK/Gradle.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
