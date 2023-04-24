plugins {
  `java-library`
}

repositories {
  mavenCentral()
}

group = "htnl5.yarl"
version = "1.0-SNAPSHOT"

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
  testImplementation("org.assertj:assertj-core:3.24.2")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

//val customCompilerArgs = listOf("--enable-preview", "--add-modules", "jdk.incubator.concurrent")

tasks.withType<JavaCompile> {
  sourceCompatibility = JavaVersion.VERSION_17.toString()
  targetCompatibility = JavaVersion.VERSION_17.toString()
//  options.compilerArgs.addAll(customCompilerArgs)
}

//tasks.withType<JavaExec> {
//  jvmArgs(customCompilerArgs)
//}

tasks.getByName<Test>("test") {
  useJUnitPlatform {
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
  }
//  jvmArgs(customCompilerArgs)
}
