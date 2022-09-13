package com.illtamer.service.currencytrading.common.util;

import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class ClassPathUtil {

    public static String readFile(String classPathFile) throws IOException {
        try (InputStream input = ClassPathUtil.class.getResourceAsStream(classPathFile)) {
            if (input == null) {
                throw new IOException("Classpath file not found: " + classPathFile);
            }
            return readAsString(input);
        }
    }

    private static String readAsString(InputStream input) throws IOException {
        return new String(readAsBytes(input), StandardCharsets.UTF_8);
    }

    private static byte[] readAsBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 1024);
        input.transferTo(output);
        return output.toByteArray();
    }

}
