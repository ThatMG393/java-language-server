package org.javacs;

import com.google.common.reflect.ClassPath;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class Classes {

    /** All exported modules in the JDK */
    static String[] JDK_MODULES = {
        "java.activation",
        "java.base",
        "java.compiler",
        "java.corba",
        "java.datatransfer",
        "java.desktop",
        "java.instrument",
        "java.jnlp",
        "java.logging",
        "java.management",
        "java.management.rmi",
        "java.naming",
        "java.prefs",
        "java.rmi",
        "java.scripting",
        "java.se",
        "java.se.ee",
        "java.security.jgss",
        "java.security.sasl",
        "java.smartcardio",
        "java.sql",
        "java.sql.rowset",
        "java.transaction",
        "java.xml",
        "java.xml.bind",
        "java.xml.crypto",
        "java.xml.ws",
        "java.xml.ws.annotation",
        "javafx.base",
        "javafx.controls",
        "javafx.fxml",
        "javafx.graphics",
        "javafx.media",
        "javafx.swing",
        "javafx.web",
        "jdk.accessibility",
        "jdk.attach",
        "jdk.charsets",
        "jdk.compiler",
        "jdk.crypto.cryptoki",
        "jdk.crypto.ec",
        "jdk.dynalink",
        "jdk.editpad",
        "jdk.hotspot.agent",
        "jdk.httpserver",
        "jdk.incubator.httpclient",
        "jdk.jartool",
        "jdk.javadoc",
        "jdk.jcmd",
        "jdk.jconsole",
        "jdk.jdeps",
        "jdk.jdi",
        "jdk.jdwp.agent",
        "jdk.jfr",
        "jdk.jlink",
        "jdk.jshell",
        "jdk.jsobject",
        "jdk.jstatd",
        "jdk.localedata",
        "jdk.management",
        "jdk.management.agent",
        "jdk.management.cmm",
        "jdk.management.jfr",
        "jdk.management.resource",
        "jdk.naming.dns",
        "jdk.naming.rmi",
        "jdk.net",
        "jdk.pack",
        "jdk.packager.services",
        "jdk.rmic",
        "jdk.scripting.nashorn",
        "jdk.sctp",
        "jdk.security.auth",
        "jdk.security.jgss",
        "jdk.snmp",
        "jdk.xml.dom",
        "jdk.zipfs",
    };

    static ClassSource jdkTopLevelClasses() {
        var classes = new HashSet<String>();
        var fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        for (var m : JDK_MODULES) {
            var moduleRoot = fs.getPath(String.format("/modules/%s/", m));
            try {
                Files.walk(moduleRoot)
                        .forEach(
                                classFile -> {
                                    var relative = moduleRoot.relativize(classFile).toString();
                                    if (relative.endsWith(".class") && !relative.contains("$")) {
                                        var trim = relative.substring(0, relative.length() - ".class".length());
                                        var qualifiedName = trim.replace(File.separatorChar, '.');
                                        classes.add(qualifiedName);
                                    }
                                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        LOG.info(String.format("Found %d classes in the java platform", classes.size()));
        class PlatformClassSource implements ClassSource {
            public Set<String> classes() {
                return Collections.unmodifiableSet(classes);
            }

            public Class<?> load(String className) {
                try {
                    return ClassLoader.getPlatformClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new PlatformClassSource();
    }

    static ClassSource classPathTopLevelClasses(Set<Path> classPath) {
        Function<Path, URL> toUrl =
                p -> {
                    try {
                        return p.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                };
        var urls = classPath.stream().map(toUrl).toArray(URL[]::new);
        var classLoader = new URLClassLoader(urls, null);
        ClassPath scanner;
        try {
            scanner = ClassPath.from(classLoader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var classes = scanner.getTopLevelClasses().stream().map(info -> info.getName()).collect(Collectors.toSet());
        LOG.info(String.format("Found %d classes in %s", classes.size(), classPath));
        class ClassPathClassSource implements ClassSource {
            public Set<String> classes() {
                return Collections.unmodifiableSet(classes);
            }

            public Class<?> load(String className) {
                try {
                    return classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new ClassPathClassSource();
    }

    private static final Logger LOG = Logger.getLogger("main");
}
