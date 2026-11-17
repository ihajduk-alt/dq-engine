plugins {
    id("dq.java-library-conventions")
    `java-test-fixtures`
}

description = "Data-quality rule engine"

dependencies {
    implementation(libs.jackson.databind)
}
