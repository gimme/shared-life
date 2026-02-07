package dev.gimme.sharedlife.infrastructure;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfigSpec {

    private CommentedFileConfig config;

    private final List<VariableBuilder> configValues = new ArrayList<>();

    private void onLoad() {
        configValues.forEach(value -> {
            if (!config.contains(value.key)) {
                if (value.comment != null) {
                    config.setComment(value.key, value.comment);
                }
                config.set(value.key, value.defaultValue);
                config.save();
            }
        });
    }

    public void init(Path configDir, String fileName) {
        config = CommentedFileConfig
                .builder(configDir.resolve(fileName), TomlFormat.instance())
                .onLoad(this::onLoad)
                .preserveInsertionOrder()
                .autoreload()
                .build();
        config.load();
    }

    public VariableBuilder variable() {
        return new VariableBuilder(this);
    }

    public static class VariableBuilder {

        private final ModConfigSpec spec;
        private @Nullable String comment;
        private String key;
        private Object defaultValue;

        private VariableBuilder(ModConfigSpec spec) {
            this.spec = spec;
        }

        public VariableBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public <T> ConfigValue<T> define(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            spec.configValues.add(this);
            return new ConfigValue<>(key, spec);
        }
    }

    public record ConfigValue<T>(String key, ModConfigSpec spec) {
        public T get() {
            if (spec.config == null) {
                throw new IllegalStateException("Config has not been initialized");
            }
            if (!spec.config.contains(key)) {
                throw new IllegalStateException("Config value " + key + " is not defined");
            }
            return spec.config.get(key);
        }
    }
}
