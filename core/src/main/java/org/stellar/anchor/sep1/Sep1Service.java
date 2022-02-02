package org.stellar.anchor.sep1;

import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.util.FileUtil;

import java.io.IOException;

public class Sep1Service {
    private final Sep1Config sep1Config;

    public Sep1Service(Sep1Config sep1Config) {
        this.sep1Config = sep1Config;
    }

    public String getStellarToml() throws IOException {
        return FileUtil.getResourceFileAsString(sep1Config.getStellarFile());
    }
}
