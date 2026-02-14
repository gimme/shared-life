package dev.gimme.sharedlife.infrastructure;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModConfigSpec {

    private static final Logger LOG = LoggerFactory.getLogger(ModConfigSpec.class);

    private CommentedFileConfig config;
    private final List<VariableBuilder> configValues = new ArrayList<>();

    private void onLoad() {
        configValues.forEach(value -> {
            if (!config.contains(value.key)) {
                if (value.comment != null) {
                    config.setComment(value.key, value.comment);
                }
                config.set(value.key, value.defaultValue.get());
                config.save();
            }
        });
    }

    public void init(Path configDir, String fileName) {
        config = CommentedFileConfig
                .builder(configDir.resolve(fileName), TomlFormat.instance())
                .onLoad(this::onLoad)
                .onAutoReload(() -> LOG.info("Config reloaded: {}", fileName))
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
        private Supplier<?> defaultValue;

        private VariableBuilder(ModConfigSpec spec) {
            this.spec = spec;
        }

        public VariableBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public <T> ConfigValue<T> define(String key, T defaultValue) {
            return define(key, () -> defaultValue);
        }

        public <T> ConfigValue<T> define(String key, Supplier<T> defaultValue) {
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
