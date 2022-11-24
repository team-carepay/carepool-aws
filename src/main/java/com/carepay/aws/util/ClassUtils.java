package com.carepay.aws.util;

import java.lang.reflect.Field;
import java.util.Optional;

public class ClassUtils {

    /**
     * Finds a field in a class, including inherited fields.
     *
     * @param clazz Class to search.
     * @param name  Name of the field.
     * @return The field, or NO_FIELD if not found.
     */
    public static Optional<Field> findField(Class<?> clazz, String name) {
        final String fieldName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        do {
            try {
                final Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return Optional.of(field);
            } catch (NoSuchFieldException e) {
                // not found, try superclass
                clazz = clazz.getSuperclass();
            }
        } while (clazz != null);
        return Optional.empty();
    }


    private ClassUtils() {
        throw new IllegalStateException();
    }
}
