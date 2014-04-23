package com.github.javafaker;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.ho.yaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class FakeValuesService {
    private static final char[] METHOD_NAME_DELIMITERS = {'_'};
    private static final Logger logger = LoggerFactory.getLogger(FakeValuesService.class);
    private final Map<String, Object> fakeValuesMap;
    private final RandomService randomService;

    public FakeValuesService(Locale locale, RandomService randomService) {
        logger.info("Using locale " + locale);

        String languageCode = locale.getLanguage();
        Map valuesMap = (Map) Yaml.load(findStream(languageCode + ".yml"));
        valuesMap = (Map) valuesMap.get(languageCode);
        fakeValuesMap = (Map<String, Object>) valuesMap.get("faker");
        this.randomService = randomService;
    }

    private InputStream findStream(String filename) {
        InputStream streamOnClass = getClass().getResourceAsStream(filename);
        if (streamOnClass != null) {
            return streamOnClass;
        }
        return getClass().getClassLoader().getResourceAsStream(filename);
    }


    /**
     * Fetch a random value from an array item specified by the key
     *
     * @param key
     * @return
     */
    public Object fetch(String key) {
        List valuesArray = (List) fetchObject(key);
        return valuesArray.get(nextInt(valuesArray.size()));
    }

    public String fetchString(String key) {
        return (String) fetch(key);
    }

    /**
     * Return the object selected by the key from yaml file.
     *
     * @param key
     *            key contains path to an object. Path segment is separated by
     *            dot. E.g. name.first_name
     * @return
     */
    public Object fetchObject(String key) {
        String[] path = key.split("\\.");
        Object currentValue = fakeValuesMap;
        for (String pathSection : path) {
            currentValue = ((Map<String, Object>) currentValue).get(pathSection);
        }
        return currentValue;
    }

    public String composite(String formatKey, String joiner, Object objectToInvokeMethodOn) {
        List<String> format = (List<String>) fetch(formatKey);

        String[] parts = new String[format.size()];
        for (int i = 0; i < parts.length; i++) {
            // remove leading colon
            String methodName = format.get(i).substring(1);
            // convert to camel case
            methodName = WordUtils.capitalizeFully(methodName, METHOD_NAME_DELIMITERS).replaceAll("_", "");
            methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);

            try {
                parts[i] = (String) objectToInvokeMethodOn.getClass().getMethod(methodName, (Class[]) null).invoke(objectToInvokeMethodOn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return StringUtils.join(parts, joiner);
    }

    public String numerify(String numberString) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < numberString.length(); i++) {
            if (numberString.charAt(i) == '#') {
                sb.append(nextInt(10));
            } else {
                sb.append(numberString.charAt(i));
            }
        }

        return sb.toString();
    }
    public String bothify(String string) {
        return letterify(numerify(string));
    }

    public String letterify(String letterString) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < letterString.length(); i++) {
            if (letterString.charAt(i) == '?') {
                sb.append((char) (97 + nextInt(26))); // a-z
            } else {
                sb.append(letterString.charAt(i));
            }
        }

        return sb.toString();
    }
    private int nextInt(int n) {
        return randomService.nextInt(n);
    }
}
