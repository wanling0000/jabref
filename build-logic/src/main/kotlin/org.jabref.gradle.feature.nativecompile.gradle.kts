plugins {
    id("org.graalvm.buildtools.native")
}

graalvmNative {
    metadataRepository {
        enabled = true
    }
    binaries {
        named("main") {
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
                "-H:IncludeLocales=en",
                "--enable-all-security-services",
                "--enable-native-access=ALL-UNNAMED",
                "--enable-url-protocols=http,https",
                // EXPERIMENT (2026-08-27): try to run the AWT/PDF path on plain GraalVM CE instead of
                // Liberica NIK. -H:Preserve registers the java.desktop JNI surface so libawt's
                // JNI_OnLoad can FindClass java/awt/GraphicsEnvironment (the Linux crash in
                // `pdf update --format=bibtex-attachment`). AddAllCharsets silences PDFBox's harmless
                // Windows-1252 UnsupportedCharsetException fallback.
                "-H:Preserve=module=java.desktop",
                "-H:+AddAllCharsets"
            )
        }
    }
}
