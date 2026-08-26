repositories {
    // Resolve from the local Maven repository (~/.m2) when experimenting locally.
    // Enabled by `useMavenLocal`, or implicitly by `javafxVersion` (a locally published JavaFX build).
    // A bare `-PuseMavenLocal` (no value) counts as true; only `-PuseMavenLocal=false` disables it.
    val useMavenLocal = providers.gradleProperty("useMavenLocal").map { it.isEmpty() || it.toBoolean() }.getOrElse(false)
    val javafxOverride = providers.gradleProperty("javafxVersion").isPresent
    if (useMavenLocal || javafxOverride) {
        mavenLocal()
    }

    mavenCentral()

    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }

    // Temporary: test PDFBox 3.0.9-SNAPSHOT (PDFBOX-6214 AWT fix) under GraalVM CE native image
    maven { url = uri("https://repository.apache.org/content/groups/snapshots/") }

    // Required for https://github.com/sialcasa/mvvmFX
    maven { url = uri("https://jitpack.io") }

    // Required for one.jpro.jproutils:tree-showing
    maven { url = uri("https://sandec.jfrog.io/artifactory/repo") }

    maven { url = rootDir.resolve("jablib/lib").toURI() }
}
