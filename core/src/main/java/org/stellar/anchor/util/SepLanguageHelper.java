package org.stellar.anchor.util;

import static org.stellar.anchor.util.Log.errorEx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.config.AppConfig;

public class SepLanguageHelper {
  static Map<String, Iso3316Language> fallbackLangs = null;

  public static String validateLanguage(AppConfig appConfig, String lang) {
    if (fallbackLangs == null) {
      fallbackLangs = prepareFallbackLanguages(appConfig);
    }

    if (lang != null) {
      List<String> languages = appConfig.getLanguages();
      if (languages != null && languages.size() > 0) {
        if (languages.stream().noneMatch(l -> l.equalsIgnoreCase(lang))) {
          return getFallbackLanguage(lang);
        }
      }
      return lang;
    } else {
      return getFallbackLanguage(lang);
    }
  }

  public static void reset() {
    fallbackLangs = null;
  }

  static Map<String, Iso3316Language> prepareFallbackLanguages(AppConfig appConfig) {
    Map<String, Iso3316Language> fallbacks = new HashMap<>();
    appConfig
        .getLanguages()
        .forEach(
            lang -> {
              try {
                Iso3316Language isoLang = Iso3316Language.of(lang);
                fallbacks.putIfAbsent(isoLang.getLangKey(), isoLang);
              } catch (SepValidationException ex) {
                errorEx(ex);
              }
            });
    return fallbacks;
  }

  static final String LANGUAGE_ONLY_DEFAULT = "en";
  static final String LANGUAGE_DEFAULT = "en-US";

  static String getFallbackLanguage(String lang) {
    try {
      Iso3316Language language = Iso3316Language.of(lang);
      Iso3316Language fallback = fallbackLangs.get(language.getLangKey());
      if (fallback == null) {
        if (language.getLocale() == null) {
          return LANGUAGE_ONLY_DEFAULT;
        }
        return LANGUAGE_DEFAULT;
      }
      return fallback.toString();
    } catch (SepValidationException e) {
      return LANGUAGE_DEFAULT;
    }
  }
}

@Getter
@AllArgsConstructor
class Iso3316Language {
  String language;
  String locale;

  public static Iso3316Language of(String lang) throws SepValidationException {
    if (lang == null) {
      throw new SepValidationException("lang is null");
    }
    String[] tokens = lang.split("-");
    switch (tokens.length) {
      case 1:
        return new Iso3316Language(tokens[0], null);
      case 2:
        return new Iso3316Language(tokens[0], tokens[1]);
      default:
        throw new SepValidationException(String.format("Invalid language format: %s", lang));
    }
  }

  public String getLangKey() {
    if (locale == null) {
      return language;
    }
    return "+" + language;
  }

  public String toString() {
    return (locale == null) ? language : language + "-" + locale;
  }
}
