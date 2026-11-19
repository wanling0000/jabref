package org.jabref.nativeimage;

import java.nio.file.Path;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/// Keeps the Java compiler (javac) out of the native image.
///
/// DJL can compile `.java` files that a model ships with (`ServingTranslatorFactory.findTranslator`).
/// That path calls `ToolProvider.getSystemJavaCompiler()`, so native-image includes all of `jdk.compiler`
/// (about 13,000 reachable methods) although JabRef never uses it and DJL disables it by default.
/// A native image cannot compile Java at runtime anyway.
@TargetClass(className = "ai.djl.util.ClassLoaderUtils")
final class Target_ai_djl_util_ClassLoaderUtils {

    @Substitute
    public static void compileJavaClass(Path dir) {
        // Same as DJL's default (compiling bundled sources is disabled): do nothing
    }
}
