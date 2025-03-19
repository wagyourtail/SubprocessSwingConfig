package xyz.wagyourtail.subprocess_config;

import xyz.wagyourtail.subprocess_config.settings.DynamicSettings;

public class ExampleSettings extends DynamicSettings {
    public final Setting<Boolean> example1 = register("example1", false);
    public final Setting<String> example2 = register("example2", "default");
    public final Setting<Integer> example3 = register("example3", 0);
    public final Setting<Double> example4 = register("example4", 0.0);

}
