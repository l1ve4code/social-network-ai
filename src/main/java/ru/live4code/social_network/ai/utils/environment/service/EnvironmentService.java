package ru.live4code.social_network.ai.utils.environment.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.environment.dao.EnvironmentDao;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentDao environmentDao;

    public void addEnvironment(String key, String value) {
        environmentDao.insertEnvironment(key, value);
    }

    public void deleteEnvironment(String key) {
        environmentDao.deleteEnvironment(key);
    }

    public long getLongValueOrDefault(String key, long defaultValue) {
        @Nullable String value = environmentDao.getEnvironmentValue(key);
        if (value == null) {
            return defaultValue;
        }

        @Nullable Long result = parseNumberOrNull(value, Long::parseLong);

        return result != null ? result : defaultValue;
    }

    public int getIntValueOrDefault(String key, int defaultValue) {
        @Nullable String value = environmentDao.getEnvironmentValue(key);
        if (value == null) {
            return defaultValue;
        }

        @Nullable Integer result = parseNumberOrNull(value, Integer::parseInt);

        return result != null ? result : defaultValue;
    }

    public boolean getBooleanValueOrDefault(String key, boolean defaultValue) {
        @Nullable String value = environmentDao.getEnvironmentValue(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private static <T extends Number> T parseNumberOrNull(String value, Function<String, T> parse) {
        try {
            return parse.apply(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
