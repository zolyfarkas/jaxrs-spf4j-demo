package org.spf4j.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nullable;
import org.spf4j.base.CharSequences;
import org.spf4j.io.csv.CharSeparatedValues;

/**
 * https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
 *
 * @author Zoltan Farkas
 */
public class HttpWarning {

  /**
   * Warning status - Response is stale.
   */
  public static final int STALE = 110;
  /**
   * Warning status - Revalidation failed.
   */
  public static final int REVALIDATION_FAILED = 111;
  /**
   * Warning status - Disconnected opertaion.
   */
  public static final int DISCONNECTED_OPERATION = 112;
  /**
   * Warning status - Heuristic expiration.
   */
  public static final int HEURISTIC_EXPIRATION = 113;
  /**
   * Warning status - Miscellaneous warning
   */
  public static final int MISCELLANEOUS = 199;
  /**
   * Warning status - Transformation applied.
   */
  public static final int TRANSFORMATION_APPLIED = 214;
  /**
   * Warning status - Miscellaneous warning
   */
  public static final int PERSISTENT_MISCELLANEOUS = 299;

  private static final CharSeparatedValues CSV = new CharSeparatedValues(' ', ',');

  private final int code;
  private final String agent;
  private final String text;
  private final ZonedDateTime date;

  /**
   * Warning = "Warning" ":" 1#warning-value
   * warning-value = warn-code SP warn-agent SP warn-text [SP warn-date]
   * warn-code = 3DIGIT
   * warn-agent = ( host [ ":" port ] ) | pseudonym
   *  ; the name or pseudonym of the server adding
   *  ; the Warning header, for use in debugging
   *  warn-text = quoted-string
   *  warn-date = <"> HTTP-date <"> ;(RFC 1123)
   *
   * @param headerValue
   * @return
   */
  public static HttpWarning parse(CharSequence headerValue) {
    Iterable<Iterable<String>> parsed = CSV.asIterable(CharSequences.reader(headerValue));
    Iterable<String> line = parsed.iterator().next();
    Iterator<String> lIt = line.iterator();
    String codeStr = lIt.next();
    int code = CharSequences.parseUnsignedInt(codeStr, 10, 0, codeStr.length());
    String agent = lIt.next();
    String text = lIt.next();
    ZonedDateTime zdt;
    if (lIt.hasNext()) {

      zdt = DateTimeFormatter.RFC_1123_DATE_TIME.parse(lIt.next(), ZonedDateTime::from);
    } else {
      zdt = null;
    }

    return new HttpWarning(code, agent, text, zdt);
  }

  public HttpWarning(final int code, final String agent, final String text) {
    this(code, agent, text, null);
  }

  public HttpWarning(final int code, final String agent, final String text, @Nullable final  ZonedDateTime date) {
    this.code = code;
    this.agent = agent;
    this.text = text;
    this.date = date == null ? null : date.withNano(0);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(32);
    try {
     if (date != null) {
       CSV.writeCsvRowNoEOL(sb, code, agent, text, DateTimeFormatter.RFC_1123_DATE_TIME.format(date));
     } else {
       CSV.writeCsvRowNoEOL(sb, code, agent, text);
     }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return sb.toString();
  }


  public int getCode() {
    return code;
  }

  public String getAgent() {
    return agent;
  }

  public String getText() {
    return text;
  }

  public ZonedDateTime getDate() {
    return date;
  }

  @Override
  public int hashCode() {
    int hash = 77 + this.code;
    hash = 11 * hash + Objects.hashCode(this.agent);
    return 11 * hash + Objects.hashCode(this.text);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final HttpWarning other = (HttpWarning) obj;
    if (this.code != other.code) {
      return false;
    }
    if (!Objects.equals(this.agent, other.agent)) {
      return false;
    }
    if (!Objects.equals(this.text, other.text)) {
      return false;
    }
    if (this.date != null) {
      if (other.date == null) {
        return false;
      } else {
        return this.date.toEpochSecond()  == other.date.toEpochSecond();
      }
    } else {
      return other.date == null;
    }
  }

}
