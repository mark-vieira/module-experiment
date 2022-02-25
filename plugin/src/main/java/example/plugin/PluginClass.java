package example.plugin;

import com.google.common.base.Joiner;
import example.platform.PlatformInterface;

public class PluginClass implements PlatformInterface {
    @Override
    public void execute() {
        System.out.println(Joiner.on(", ").join("Hello", "World"));
    }
}
