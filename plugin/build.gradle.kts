plugins {
    `java-library`
}

dependencies {
    implementation(project(":platform-jar"))
    implementation("com.google.guava:guava:31.0.1-jre")
}