plugins {
    id("dq.java-library-conventions")
    `java-test-fixtures`
}

description = "Data-quality rule engine"

dependencies {
    implementation(libs.jackson.databind)
}

// The scale test runs separately, in a heap too small to hold its results.
tasks.test {
    useJUnitPlatform {
        excludeTags("scale")
    }
}

val scaleTest = tasks.register<Test>("scaleTest") {
    description = "Validates 1,000,000 generated records in a 64 MB heap."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("scale")
    }
    maxHeapSize = "64m"
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(scaleTest)
}
