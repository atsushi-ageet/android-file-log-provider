import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
    signing
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(30)

        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = false
        resValues = false
    }
}

dependencies {
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                pom {
                    name.set("File Log Provider")
                    description.set("file log library for android")
                    url.set("https://github.com/atsushi-ageet/android-file-log-provider")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("atsushi")
                            name.set("Atsushi Yamauchi")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/atsushi-ageet/android-file-log-provider.git")
                        developerConnection.set("scm:git:ssh:git@github.com:atsushi-ageet/android-file-log-provider.git")
                        url.set("https://github.com/atsushi-ageet/android-file-log-provider/tree/master")
                    }
                }
            }
        }
    }
    signing {
        sign(publishing.publications["release"])
    }
}
