import com.google.protobuf.gradle.*

val ktorVersion = "1.6.6"
val logstashEncoderVersion = "7.0.1"
val kotlinVersion = "1.3.40"
val junitJupiterVersion = "5.8.2"
val protoGradlePluginVersion = "0.8.18"
val protocVersion = "3.19.1"
val grpcVersion = "1.42.1"
val grpcKotlinVersion = "1.2.0"
val tikaVersion = "2.1.0"

group = "com.github.esiqveland"
version = "1.0"

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.parmet.buf") version "0.3.1"
    id("com.google.protobuf") version "0.8.18"
    id("idea")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.0")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.vavr:vavr:1.0.0-alpha-4")
    implementation("org.rocksdb:rocksdbjni:6.26.1")

    implementation("io.reactivex.rxjava3:rxjava:3.1.3")

    implementation("org.apache.tika:tika-parsers-standard-package:$tikaVersion")
    implementation("org.apache.tika:tika-parsers:$tikaVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("org.apache.tika:tika-langdetect-optimaize:$tikaVersion")
//    implementation("org.apache.tika:tika-parsers-scientific-module:$tikaVersion")

    implementation("com.github.hypfvieh:dbus-java:3.3.1")

    implementation("com.google.protobuf:protobuf-java:$protocVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53") // necessary for Java 9+

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")

    implementation("ch.qos.logback:logback-classic:1.3.0-alpha10")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "junit")
    }
}

java {
    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
}

tasks {
    compileJava {
        options.compilerArgs.add("--enable-preview")
    }
    test {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    named<Jar>("jar") {
        archiveBaseName.set("hugin")

        manifest {
            attributes["Main-Class"] = "com.github.esiqveland.Main"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.2"
    }
}

// Inform IDEs like IntelliJ IDEA, Eclipse or NetBeans about the generated code.
sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/java")
            srcDirs("build/generated/source/proto/main/grpc")
            //srcDirs("build/generated/source/proto/main/grpckt")
        }
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        //id("grpckt") {
        //    artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
        //}
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
                //id("grpckt")
            }
        }
    }
}
