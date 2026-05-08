package org.wayland4j.protocol.apt;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.wayland4j.protocol.model.Interface;
import org.wayland4j.protocol.model.Protocol;
import org.wayland4j.protocol.scanner.XmlProtocolParser;

@SupportedAnnotationTypes("org.wayland4j.protocol.apt.GenerateWaylandClient")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class WaylandClientApProcessor extends AbstractProcessor {

    /** Packages we've already generated, in the order they were processed; value = isCore. */
    private final Map<String, Boolean> generatedPackages = new LinkedHashMap<>();
    /** Global interface name -> owning Java package, accumulated across rounds. */
    private final Map<String, String> interfaceToPackage = new HashMap<>();
    private boolean aggregatorEmitted;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
    }

    private record PendingProtocol(String pkg, Protocol protocol, boolean core) {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Pass 1: parse every annotated package's XML so we have a complete
        // interface -> package map before emitting any source. This is what
        // lets cross-protocol references (e.g. xdg-shell.xdg_surface uses
        // wl_surface) qualify with the right package.
        List<PendingProtocol> pending = new ArrayList<>();
        for (Element annotated : roundEnv.getElementsAnnotatedWith(GenerateWaylandClient.class)) {
            if (!(annotated instanceof PackageElement pkg)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@GenerateWaylandClient must be on a package", annotated);
                continue;
            }
            String pkgName = pkg.getQualifiedName().toString();
            if (generatedPackages.containsKey(pkgName)) {
                continue;
            }
            GenerateWaylandClient ann = pkg.getAnnotation(GenerateWaylandClient.class);
            String xmlResource = ann.xml();
            try {
                Protocol protocol = loadProtocol(xmlResource);
                pending.add(new PendingProtocol(pkgName, protocol, ann.core()));
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "wayland4j: failed to read " + xmlResource + ": " + e, annotated);
            } catch (RuntimeException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "wayland4j: code generation failed: " + e, annotated);
                throw e;
            }
        }

        for (PendingProtocol p : pending) {
            for (Interface i : p.protocol().interfaces()) {
                interfaceToPackage.putIfAbsent(i.name(), p.pkg());
            }
        }

        // Pass 2: emit each protocol's sources with full cross-reference info.
        for (PendingProtocol p : pending) {
            try {
                generate(p.pkg(), p.protocol());
                generatedPackages.put(p.pkg(), p.core());
            } catch (RuntimeException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "wayland4j: code generation failed for " + p.pkg() + ": " + e);
                throw e;
            }
        }

        if (roundEnv.processingOver() && !aggregatorEmitted && !generatedPackages.isEmpty()) {
            emitAggregator();
            aggregatorEmitted = true;
        }
        return false;
    }

    private Protocol loadProtocol(String resource) throws IOException {
        ClassLoader cl = WaylandClientApProcessor.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("resource not on AP classpath: " + resource);
            }
            return XmlProtocolParser.parse(in);
        }
    }

    private void generate(String pkgName, Protocol protocol) {
        emit(pkgName, "WaylandProtocols", new ProtocolsClassEmitter(pkgName, protocol).emit());
        for (Interface iface : protocol.interfaces()) {
            String typeName = org.wayland4j.protocol.scanner.JavaIdentifiers.typeName(iface.name());
            emit(pkgName, typeName, new InterfaceClassEmitter(pkgName, iface, interfaceToPackage).emit());
        }
    }

    private void emitAggregator() {
        // Core packages must load before any extension that references their interfaces
        // (e.g. xdg-shell uses wl_surface). Order: cores first, then extensions, each
        // group preserving processing order.
        List<String> cores = new ArrayList<>();
        List<String> extensions = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : generatedPackages.entrySet()) {
            (Boolean.TRUE.equals(e.getValue()) ? cores : extensions).add(e.getKey());
        }
        List<String> ordered = new ArrayList<>(cores.size() + extensions.size());
        ordered.addAll(cores);
        ordered.addAll(extensions);

        StringBuilder sb = new StringBuilder(2048);
        sb.append("// Generated by wayland4j: do not edit.\n");
        sb.append("package org.wayland4j.client.internal;\n\n");
        sb.append("public final class GeneratedRegistry {\n");
        sb.append("    private GeneratedRegistry() {}\n\n");
        sb.append("    static {\n");
        for (String pkg : ordered) {
            sb.append("        load(\"").append(pkg).append(".WaylandProtocols\");\n");
        }
        sb.append("    }\n\n");
        sb.append("    /** Triggered by Wayland.ensureBootstrapped via Class.forName. */\n");
        sb.append("    public static void touch() { /* loading this class runs <clinit> */ }\n\n");
        sb.append("    private static void load(String fqn) {\n");
        sb.append("        try { Class.forName(fqn, true, GeneratedRegistry.class.getClassLoader()); }\n");
        sb.append("        catch (ClassNotFoundException e) { throw new IllegalStateException(\n");
        sb.append("            \"wayland4j: missing generated protocol class \" + fqn, e); }\n");
        sb.append("    }\n");
        sb.append("}\n");

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile("org.wayland4j.client.internal.GeneratedRegistry");
            try (Writer w = file.openWriter(); PrintWriter out = new PrintWriter(w)) {
                out.write(sb.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "wayland4j: cannot write GeneratedRegistry: " + e);
        }
    }

    private void emit(String pkg, String simpleName, String source) {
        String qualified = pkg + "." + simpleName;
        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(qualified);
            try (Writer w = file.openWriter(); PrintWriter out = new PrintWriter(w)) {
                out.write(source);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "wayland4j: cannot write " + qualified + ": " + e);
        }
    }
}
