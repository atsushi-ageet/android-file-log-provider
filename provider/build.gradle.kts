import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
    signing
}

val commonCompileSdkVersion: Int by project
val commonMinSdkVersion: Int by project
val commonTargetSdkVersion: Int by project

android {
    namespace = "com.ageet.filelogprovider"
    compileSdk = commonTargetSdkVersion

    defaultConfig {
        minSdk = commonMinSdkVersion
        targetSdk = commonTargetSdkVersion

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
        repositories {
            maven {
                name = "sonatype"
                val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                logger.quiet("sonatype: $url")
                credentials {
                    username = project.properties["sonatype.user"].toString()
                    password = project.properties["sonatype.password"].toString()
                }
            }
        }
    }
    signing {
        sign(publishing.publications["release"])
    }
}
