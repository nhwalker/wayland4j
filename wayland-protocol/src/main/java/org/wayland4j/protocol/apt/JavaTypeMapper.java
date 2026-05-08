package org.wayland4j.protocol.apt;

import org.wayland4j.protocol.model.Arg;
import org.wayland4j.protocol.model.ArgType;
import org.wayland4j.protocol.scanner.JavaIdentifiers;

/**
 * Maps Wayland XML argument types to Java types for generated proxy methods
 * and listener interfaces.
 */
final class JavaTypeMapper {

    private final String targetPackage;

    JavaTypeMapper(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    /** Java parameter type for a request argument. */
    String requestParamType(Arg arg) {
        return switch (arg.type()) {
            case INT, UINT, FIXED, FD -> "int";
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
            case INT, UINT, FIXED, FD -> "int";
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
            case INT, UINT, FIXED, FD -> "Integer";
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
        return targetPackage + "." + JavaIdentifiers.typeName(wlInterfaceName);
    }
}
