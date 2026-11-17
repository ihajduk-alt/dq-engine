plugins {
    id("dq.java-conventions")
    application
}

description = "Example host: a small HTTP API that validates a batch of records with the library."

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":dq-engine"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.expression)

    testImplementation(testFixtures(project(":dq-engine")))
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.test)
    testImplementation(libs.wiremock)
}

application {
    mainClass = "io.dqengine.example.http.ExampleApplication"
}
