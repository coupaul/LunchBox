package org.apache.commons.lang3.text;

import java.text.Format;
import java.util.Locale;

public interface FormatFactory {

    Format getFormat(String s, String s1, Locale locale);
}
