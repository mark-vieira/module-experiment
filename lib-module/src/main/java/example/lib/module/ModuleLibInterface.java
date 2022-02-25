package example.lib.module;

import org.apache.commons.text.RandomStringGenerator;

public interface ModuleLibInterface {
    default String getString() {
        return new RandomStringGenerator.Builder().build().generate(10);
    }
}
