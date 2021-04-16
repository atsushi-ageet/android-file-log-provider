buildscript {
    apply(from = "android-config.gradle")

    repositories {
        google()
        mavenCentral()
        jcenter().mavenContent {
            includeGroup("org.jetbrains.trove4j")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath(kotlin("gradle-plugin", version = "1.4.32"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
