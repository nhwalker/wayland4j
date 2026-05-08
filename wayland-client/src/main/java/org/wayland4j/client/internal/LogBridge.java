package org.wayland4j.client.internal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes libwayland's client-side {@code wl_log_func_t} through
 * {@link java.util.logging}. va_list isn't directly representable in FFM, so we
 * format the message inside the upcall via libc's {@code vsnprintf}.
 */
final class LogBridge {

    private static final Logger LOG = Logger.getLogger("org.wayland4j.client.libwayland");
    private static final int BUFFER_SIZE = 2048;

    private static final FunctionDescriptor WL_LOG_FUNC = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // const char *fmt
            ValueLayout.ADDRESS  // va_list ap
    );

    private LogBridge() {
    }

    static void install() {
        MethodHandle vsnprintf = bindVsnprintf();
        if (vsnprintf == null) {
            // libc / vsnprintf unavailable — give up on log routing rather than fail link-time.
            return;
        }
        Optional<MemorySegment> setHandler = NativeLibrary.LOOKUP.find("wl_log_set_handler_client");
        if (setHandler.isEmpty()) {
            return;
        }
        MethodHandle setHandlerMh = NativeLibrary.LINKER.downcallHandle(
                setHandler.get(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(
                    LogBridge.class, "onLog",
                    MethodType.methodType(void.class, MethodHandle.class, MemorySegment.class, MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        MethodHandle bound = target.bindTo(vsnprintf);
        MemorySegment stub = NativeLibrary.LINKER.upcallStub(bound, WL_LOG_FUNC, NativeLibrary.LIB_ARENA);
        try {
            setHandlerMh.invokeExact(stub);
        } catch (Throwable t) {
            // Don't let log-handler installation break startup.
            LOG.log(Level.WARNING, "wl_log_set_handler_client failed; libwayland log routing disabled", t);
        }
    }

    private static MethodHandle bindVsnprintf() {
        // libc is implicitly mapped through the default lookup on Linux.
        SymbolLookup defaultLookup = NativeLibrary.LINKER.defaultLookup();
        Optional<MemorySegment> sym = defaultLookup.find("vsnprintf");
        if (sym.isEmpty()) return null;
        return NativeLibrary.LINKER.downcallHandle(
                sym.get(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,                // char *str
                        ValueLayout.JAVA_LONG,              // size_t size
                        ValueLayout.ADDRESS,                // const char *fmt
                        ValueLayout.ADDRESS                 // va_list ap
                ));
    }

    @SuppressWarnings("unused") // upcall target
    private static void onLog(MethodHandle vsnprintf, MemorySegment fmt, MemorySegment va) {
        if (!LOG.isLoggable(Level.WARNING)) return;
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment buf = scratch.allocate(BUFFER_SIZE, 1);
            int written;
            try {
                written = (int) vsnprintf.invokeExact(buf, (long) BUFFER_SIZE, fmt, va);
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "libwayland log message (formatting failed)", t);
                return;
            }
            if (written < 0) {
                LOG.warning("libwayland log message (vsnprintf returned " + written + ")");
                return;
            }
            int len = Math.min(written, BUFFER_SIZE - 1);
            byte[] bytes = new byte[len];
            MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, 0, bytes, 0, len);
            String msg = new String(bytes, StandardCharsets.UTF_8);
            // libwayland appends a trailing newline; trim it for the JUL message.
            if (!msg.isEmpty() && msg.charAt(msg.length() - 1) == '\n') {
                msg = msg.substring(0, msg.length() - 1);
            }
            LOG.warning(msg);
        }
    }
}
