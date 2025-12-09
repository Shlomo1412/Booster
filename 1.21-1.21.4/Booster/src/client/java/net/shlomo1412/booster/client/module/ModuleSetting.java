package net.shlomo1412.booster.client.module;

import java.util.function.Consumer;

/**
 * Base class for module settings that can be edited in the editor sidebar.
 * Supports different types: color, enum/dropdown, number, boolean.
 */
public abstract class ModuleSetting<T> {
    private final String id;
    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private Consumer<T> onChange;

    public ModuleSetting(String id, String name, String description, T defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public T getValue() { return value; }
    public T getDefaultValue() { return defaultValue; }

    public void setValue(T value) {
        this.value = value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    public void setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
    }

    public void reset() {
        setValue(defaultValue);
    }

    /**
     * @return The type of this setting for UI rendering
     */
    public abstract SettingType getType();

    public enum SettingType {
        COLOR,
        ENUM,
        NUMBER,
        BOOLEAN
    }

    /**
     * Color setting with ARGB value.
     */
    public static class ColorSetting extends ModuleSetting<Integer> {
        public ColorSetting(String id, String name, String description, int defaultValue) {
            super(id, name, description, defaultValue);
        }

        @Override
        public SettingType getType() {
            return SettingType.COLOR;
        }
    }

    /**
     * Enum/dropdown setting with predefined options.
     */
    public static class EnumSetting<E extends Enum<E>> extends ModuleSetting<E> {
        private final Class<E> enumClass;

        public EnumSetting(String id, String name, String description, E defaultValue, Class<E> enumClass) {
            super(id, name, description, defaultValue);
            this.enumClass = enumClass;
        }

        public E[] getOptions() {
            return enumClass.getEnumConstants();
        }

        public Class<E> getEnumClass() {
            return enumClass;
        }

        @Override
        public SettingType getType() {
            return SettingType.ENUM;
        }
    }

    /**
     * Number setting with min/max bounds.
     */
    public static class NumberSetting extends ModuleSetting<Integer> {
        private final int min;
        private final int max;

        public NumberSetting(String id, String name, String description, int defaultValue, int min, int max) {
            super(id, name, description, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() { return min; }
        public int getMax() { return max; }

        @Override
        public void setValue(Integer value) {
            super.setValue(Math.max(min, Math.min(max, value)));
        }

        @Override
        public SettingType getType() {
            return SettingType.NUMBER;
        }
    }

    /**
     * Boolean toggle setting.
     */
    public static class BooleanSetting extends ModuleSetting<Boolean> {
        public BooleanSetting(String id, String name, String description, boolean defaultValue) {
            super(id, name, description, defaultValue);
        }

        @Override
        public SettingType getType() {
            return SettingType.BOOLEAN;
        }
    }
}
