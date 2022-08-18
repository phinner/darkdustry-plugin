plugins {
    java
}

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    val mindustryVersion = "v137"
    project.version = "2.3"

    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.Anuken.Mindustry:core:$mindustryVersion")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")
    implementation("net.dv8tion:JDA:5.0.0-alpha.17")
    implementation("redis.clients:jedis:4.2.3")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    /*from(resources) {
        include("plugin.json")
    }*/
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}