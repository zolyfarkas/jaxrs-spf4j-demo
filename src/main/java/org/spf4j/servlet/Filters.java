
package org.spf4j.servlet;

import javax.servlet.FilterConfig;

/**
 *
 * @author Zoltan Farkas
 */
public final class Filters {

  private Filters() { }

  public static long getLongParameter(final FilterConfig filterConfig, final String configKey,
          final long defaultValue) {
    String value = filterConfig.getInitParameter(configKey);
    if (value == null) {
      return defaultValue;
    } else {
      return Long.parseLong(value);
    }
  }

  public static String getStringParameter(final FilterConfig filterConfig, final String configKey,
          final String defaultValue) {
    String value = filterConfig.getInitParameter(configKey);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

}
