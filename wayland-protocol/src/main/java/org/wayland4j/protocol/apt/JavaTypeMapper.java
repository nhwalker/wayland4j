package org.wayland4j.protocol.apt;

import java.util.Map;
import org.wayland4j.protocol.model.Arg;
import org.wayland4j.protocol.model.ArgType;
import org.wayland4j.protocol.scanner.JavaIdentifiers;

/**
 * Maps Wayland XML argument types to Java types for generated proxy methods
 * and listener interfaces. Cross-protocol references resolve via the supplied
 * {@code interface name -> owning Java package} map; references that aren't
 * known fall back to the current target package.
 */
final class JavaTypeMapper {

    private final String targetPackage;
    private final Map<String, String> interfaceToPackage;

    JavaTypeMapper(String targetPackage, Map<String, String> interfaceToPackage) {
        this.targetPackage = targetPackage;
        this.interfaceToPackage = interfaceToPackage;
    }

    /** Java parameter type for a request argument. */
    String requestParamType(Arg arg) {
        return switch (arg.type()) {
            case INT, UINT, FD -> "int";
            case FIXED -> "double";
            case STRING -> "String";
            case ARRAY -> "byte[]";
            case OBJECT -> arg.interfaceName() == null
                    ? "org.wayland4j.client.Proxy"
                    : qualified(arg.interfaceName());
            case NEW_ID -> "/* new_id */";
        };
    }

    /** Java parameter type for an event listener argument. */
    String eventParamType(Arg arg) {
        return switch (arg.type()) {
            case INT, UINT, FD -> "int";
            case FIXED -> "double";
            case STRING -> "String";
            case ARRAY -> "byte[]";
            case OBJECT -> arg.interfaceName() == null
                    ? "org.wayland4j.client.Proxy"
                    : qualified(arg.interfaceName());
            case NEW_ID -> arg.interfaceName() == null
                    ? "org.wayland4j.client.Proxy"
                    : qualified(arg.interfaceName());
        };
    }

    /** Boxed type used to cast args[] in the dispatcher event lambda. */
    String eventCastType(Arg arg) {
        return switch (arg.type()) {
            case INT, UINT, FD -> "Integer";
            case FIXED -> "Integer";
            case STRING -> "String";
            case ARRAY -> "byte[]";
            case OBJECT -> arg.interfaceName() == null ? "org.wayland4j.client.Proxy" : qualified(arg.interfaceName());
            case NEW_ID -> arg.interfaceName() == null ? "org.wayland4j.client.Proxy" : qualified(arg.interfaceName());
        };
    }

    /** Whether the cast needs a primitive unboxing step on top of the (Integer) cast. */
    boolean unboxesToInt(ArgType t) {
        return t == ArgType.INT || t == ArgType.UINT || t == ArgType.FIXED || t == ArgType.FD;
    }

    String qualified(String wlInterfaceName) {
        String pkg = interfaceToPackage.getOrDefault(wlInterfaceName, targetPackage);
        return pkg + "." + JavaIdentifiers.typeName(wlInterfaceName);
    }
}
