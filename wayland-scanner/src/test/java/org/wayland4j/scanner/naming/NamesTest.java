package org.wayland4j.scanner.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamesTest {

    @Test
    void pascalCase() {
        assertEquals("WlSurface", Names.pascal("wl_surface"));
        assertEquals("Attach", Names.pascal("attach"));
        assertEquals("DataDeviceManager", Names.pascal("data_device_manager"));
    }

    @Test
    void camelCase() {
        assertEquals("attach", Names.camel("attach"));
        assertEquals("attachBuffer", Names.camel("attach_buffer"));
    }

    @Test
    void camelEscapesJavaKeywords() {
        assertEquals("class_", Names.camel("class"));
        assertEquals("interface_", Names.camel("interface"));
        assertEquals("default_", Names.camel("default"));
        assertEquals("new_", Names.camel("new"));
    }

    @Test
    void enumConstantUppercases() {
        assertEquals("FORMAT_ARGB8888", Names.enumConstant("format_argb8888"));
        assertEquals("DAMAGE", Names.enumConstant("damage"));
    }

    @Test
    void enumConstantLeadingDigitPrefixed() {
        assertEquals("_2_2", Names.enumConstant("2_2"));
    }

    @Test
    void enumConstantHyphenToUnderscore() {
        assertEquals("READ_ONLY", Names.enumConstant("read-only"));
    }

    @Test
    void packageSegmentSanitises() {
        assertEquals("wayland", Names.packageSegment("wayland"));
        assertEquals("xdg_shell_v1", Names.packageSegment("xdg-shell-v1"));
        assertEquals("_3d_widgets", Names.packageSegment("3d_widgets"));
    }

    @Test
    void escapeIsIdempotentForNonKeywords() {
        assertEquals("foo", Names.escape("foo"));
        assertEquals("class_", Names.escape("class"));
    }
}
