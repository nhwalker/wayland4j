package org.wayland4j.protocol.apt;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashSet;
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

    private final Set<String> generatedPackages = new LinkedHashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotated : roundEnv.getElementsAnnotatedWith(GenerateWaylandClient.class)) {
            if (!(annotated instanceof PackageElement pkg)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@GenerateWaylandClient must be on a package", annotated);
                continue;
            }
            String pkgName = pkg.getQualifiedName().toString();
            if (!generatedPackages.add(pkgName)) {
                continue;
            }
            GenerateWaylandClient ann = pkg.getAnnotation(GenerateWaylandClient.class);
            String xmlResource = ann.xml();
            try {
                Protocol protocol = loadProtocol(xmlResource);
                generate(pkgName, protocol);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "wayland4j: failed to read " + xmlResource + ": " + e, annotated);
            } catch (RuntimeException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "wayland4j: code generation failed: " + e, annotated);
                throw e;
            }
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
            emit(pkgName, typeName, new InterfaceClassEmitter(pkgName, iface).emit());
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
