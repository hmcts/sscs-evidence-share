package helper;

import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.SystemProfileValueSource;

public class EnvironmentProfileValueSource implements ProfileValueSource {

    private final SystemProfileValueSource systemProfileValueSource = SystemProfileValueSource.getInstance();

    public String get(String key) {

        if ("environment.shared-ccd".equals(key)) {
            return String.valueOf(isPreviewOrAatEnv());
        }

        return systemProfileValueSource.get(key);
    }

    private boolean isPreviewOrAatEnv() {
        final String testUrl = System.getenv().getOrDefault("TEST_URL", "");
        return testUrl.contains("preview.internal") || testUrl.contains("aat.internal");
    }
}
