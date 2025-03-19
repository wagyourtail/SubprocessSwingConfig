package xyz.wagyourtail.subprocess_config.settings;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DynamicSettings {
    private static final System.Logger LOGGER = System.getLogger(DynamicSettings.class.getName());

    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();

    public Collection<Setting<?>> getSettings() {
        return this.settings.values();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void copyTo(DynamicSettings other) {
        for (Map.Entry<String, Setting<?>> entry : this.settings.entrySet()) {
            Setting otherSetting = other.settings.get(entry.getKey());
            if (otherSetting != null) {
                otherSetting.set(entry.getValue().get());
            }
        }
    }

    public <T extends DynamicSettings> T copyFrom(T other) {
        other.copyTo(this);
        return (T) this;
    }

    public <T> Setting<T> register(String name, Setting<T> setting) {
        this.settings.put(name, setting);
        return setting;
    }

    public <T extends Number> Setting<T> register(String name, T defaultValue) {
        return this.register(name, new PrimitiveSetting<>(name, defaultValue));
    }

    public Setting<Integer> register(String name, int defaultValue, int min, int max) {
        return this.register(name, new BoundedIntSetting(name, defaultValue, min, max));
    }

    public Setting<Double> register(String name, double defaultValue, double min, double max) {
        return this.register(name, new BoundedDoubleSetting(name, defaultValue, min, max));
    }

    public Setting<String> register(String name, String defaultValue) {
        return this.register(name, new StringSetting(name, defaultValue));
    }

    public Setting<Character> register(String name, Character defaultValue) {
        return this.register(name, new CharSetting(name, defaultValue));
    }

    public Setting<Boolean> register(String name, Boolean defaultValue) {
        return this.register(name, new BooleanSetting(name, defaultValue));
    }

    public <T extends Enum<T>> Setting<T> register(String name, T defaultValue) {
        return this.register(name, new EnumSetting<>(name, defaultValue, (Class<T>) defaultValue.getClass()));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T, U extends Setting<T>> ListSetting<T, U> registerList(String name, Function<T, ? extends Setting<T>> settingConstructor, T... defaultValue) {
        this.settings.put(name, new ListSetting<>(name, settingConstructor, defaultValue));
        return (ListSetting<T, U>) this.settings.get(name);
    }

    @SuppressWarnings("unchecked")
    public final <T, U extends Setting<T>> MapSetting<T, U> registerMap(String name, Function<T, ? extends Setting<T>> settingConstructor, Map<String, T> defaultValue) {
        this.settings.put(name, new MapSetting<>(name, defaultValue, settingConstructor));
        return (MapSetting<T, U>) this.settings.get(name);
    }

    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        for (Map.Entry<String, Setting<?>> entry : this.settings.entrySet()) {
            writer.name(entry.getKey());
            entry.getValue().serialize(writer);
        }
        writer.endObject();
    }

    public void deserialize(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            Setting<?> setting = this.settings.get(key);
            if (setting == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Unknown setting: {}", key);
                reader.skipValue();
                continue;
            }
            setting.deserialize(reader);
        }
        reader.endObject();
    }


    public static abstract class Setting<T> {
        protected final Class<T> type;
        private final String name;
        private T value;

        public Setting(String name, T defaultValue, Class<T> type) {
            this.name = name;
            this.value = defaultValue;
            this.type = type;
        }

        public T get() {
            return this.value;
        }

        public void set(T value) {
            this.value = value;
        }

        public String getName() {
            return this.name;
        }

        public abstract void serialize(JsonWriter writer) throws IOException;

        public abstract void deserialize(JsonReader value) throws IOException;
    }

    public static class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String name, Boolean defaultValue) {
            super(name, defaultValue, Boolean.class);
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.value(get());
        }

        @Override
        public void deserialize(JsonReader value) throws IOException {
            set(value.nextBoolean());
        }
    }

    public static class PrimitiveSetting<T extends Number> extends Setting<T> {
        @SuppressWarnings("unchecked")
        public PrimitiveSetting(String name, T defaultValue) {
            super(name, defaultValue, (Class<T>) defaultValue.getClass());
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            switch (type.getSimpleName()) {
                case "int", "Integer" -> writer.value(get().intValue());
                case "long", "Long" -> writer.value(get().longValue());
                case "short", "Short" -> writer.value(get().shortValue());
                case "byte", "Byte" -> writer.value(get().byteValue());
                case "float", "Float" -> writer.value(get().floatValue());
                case "double", "Double" -> writer.value(get().doubleValue());
                default -> throw new RuntimeException("Invalid type: " + type.getName());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void deserialize(JsonReader value) throws IOException {
            switch (type.getSimpleName()) {
                case "int", "Integer" -> set((T) Integer.valueOf(value.nextInt()));
                case "long", "Long" -> set((T) Long.valueOf(value.nextLong()));
                case "short", "Short" -> set((T) Short.valueOf((short) value.nextInt()));
                case "byte", "Byte" -> set((T) Byte.valueOf((byte) value.nextInt()));
                case "float", "Float" -> set((T) Float.valueOf((float) value.nextDouble()));
                case "double", "Double" -> set((T) Double.valueOf(value.nextDouble()));
                default -> throw new RuntimeException("Invalid type: " + type.getName());
            }
        }
    }

    public static class BoundedIntSetting extends PrimitiveSetting<Integer> {
        private final int min;
        private final int max;

        public BoundedIntSetting(String name, int defaultValue, int min, int max) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return this.min;
        }

        public int getMax() {
            return this.max;
        }

        @Override
        public void set(Integer value) {
            if (value < this.min || value > this.max) {
                throw new IllegalArgumentException("Value out of bounds: " + value);
            }
            super.set(value);
        }
    }

    public static class BoundedDoubleSetting extends PrimitiveSetting<Double> {
        private final double min;
        private final double max;

        public BoundedDoubleSetting(String name, double defaultValue, double min, double max) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return this.min;
        }

        public double getMax() {
            return this.max;
        }

        @Override
        public void set(Double value) {
            if (value < this.min || value > this.max) {
                throw new IllegalArgumentException("Value out of bounds: " + value);
            }
            super.set(value);
        }
    }

    public static class StringSetting extends Setting<String> {
        public StringSetting(String name, String defaultValue) {
            super(name, defaultValue, String.class);
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.value(get());
        }

        @Override
        public void deserialize(JsonReader value) throws IOException {
            set(value.nextString());
        }
    }

    public static class CharSetting extends Setting<Character> {
        public CharSetting(String name, Character defaultValue) {
            super(name, defaultValue, Character.class);
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.value(get());
        }

        @Override
        public void deserialize(JsonReader value) throws IOException {
            set(value.nextString().charAt(0));
        }
    }

    public static class ListSetting<T, U extends Setting<T>> extends Setting<List<U>> {
        protected final Class<T> elementType;
        protected final Function<T, U> settingConstructor;

        @SafeVarargs
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ListSetting(String name, Function<T, U> settingConstructor, T... defaultValue) {
            super(name, new ArrayList<>(Arrays.stream(defaultValue).map(settingConstructor).collect(Collectors.toList())), (Class) List.class);
            this.settingConstructor = settingConstructor;
            this.elementType = (Class<T>) defaultValue.getClass().getComponentType();
        }

        public void add(T value) {
            get().add(this.settingConstructor.apply(value));
        }

        public T get(int index) {
            return get().get(index).get();
        }

        public Iterator<T> iterator() {
            return new Iterator<>() {
                private final Iterator<U> iterator = get().iterator();

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public T next() {
                    return this.iterator.next().get();
                }
            };
        }

        public void remove(int index) {
            get().remove(index);
        }

        public void insert(int index, T value) {
            get().add(index, this.settingConstructor.apply(value));
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginArray();
            for (Setting<T> setting : get()) {
                setting.serialize(writer);
            }
            writer.endArray();
        }

        @Override
        public void deserialize(JsonReader value) throws IOException {
            value.beginArray();
            while (value.hasNext()) {
                U setting = this.settingConstructor.apply(null);
                setting.deserialize(value);
                get().add(setting);
            }
            value.endArray();
        }
    }

    public static class MapSetting<T, U extends Setting<T>> extends Setting<Map<String, Setting<T>>> {
        private final Function<T, U> settingConstructor;

        @SuppressWarnings({"rawtypes", "unchecked"})
        public MapSetting(String name, Map<String, T> defaultValue, Function<T, U> settingConstructor) {
            super(name, new HashMap<>(defaultValue.entrySet().stream().map(e -> Map.entry(e.getKey(), settingConstructor.apply(e.getValue()))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))), (Class) Map.class);
            this.settingConstructor = settingConstructor;
        }

        public void put(String key, T value) {
            get().put(key, this.settingConstructor.apply(value));
        }

        public T get(String key) {
            return get().get(key).get();
        }

        public void remove(String key) {
            get().remove(key);
        }

        public Set<String> keySet() {
            return get().keySet();
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            for (Map.Entry<String, Setting<T>> entry : get().entrySet()) {
                writer.name(entry.getKey());
                entry.getValue().serialize(writer);
            }
            writer.endObject();
        }

        @Override
        public void deserialize(JsonReader value) throws IOException {
            value.beginObject();
            while (value.hasNext()) {
                String key = value.nextName();
                Setting<T> setting = get().get(key);
                if (setting == null) {
                    throw new IOException("Unknown setting: " + key);
                }
                setting.deserialize(value);
            }
            value.endObject();
        }
    }

    public static class EnumSetting<E extends Enum<E>> extends Setting<E> {
        public EnumSetting(String name, E defaultValue, Class<E> type) {
            super(name, defaultValue, type);
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.value(this.get().name());
        }

        @Override
        public void deserialize(JsonReader value) throws IOException {
            this.set(Enum.valueOf(this.type, value.nextString()));
        }
    }

}
