package org.bukkit.craftbukkit.libs.jline.console.completer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.bukkit.craftbukkit.libs.jline.console.ConsoleReader;
import org.bukkit.craftbukkit.libs.jline.console.CursorBuffer;

public class CandidateListCompletionHandler implements CompletionHandler {

    public boolean complete(ConsoleReader reader, List candidates, int pos) throws IOException {
        CursorBuffer buf = reader.getCursorBuffer();

        if (candidates.size() == 1) {
            CharSequence value1 = (CharSequence) candidates.get(0);

            if (value1.equals(buf.toString())) {
                return false;
            } else {
                setBuffer(reader, value1, pos);
                return true;
            }
        } else {
            if (candidates.size() > 1) {
                String value = this.getUnambiguousCompletions(candidates);

                setBuffer(reader, value, pos);
            }

            printCandidates(reader, candidates);
            reader.drawLine();
            return true;
        }
    }

    public static void setBuffer(ConsoleReader reader, CharSequence value, int offset) throws IOException {
        while (reader.getCursorBuffer().cursor > offset && reader.backspace()) {
            ;
        }

        reader.putString(value);
        reader.setCursorPosition(offset + value.length());
    }

    public static void printCandidates(ConsoleReader reader, Collection candidates) throws IOException {
        HashSet distinct = new HashSet((Collection) candidates);

        if (distinct.size() > reader.getAutoprintThreshold()) {
            reader.print((CharSequence) CandidateListCompletionHandler.Messages.DISPLAY_CANDIDATES.format(new Object[] { Integer.valueOf(((Collection) candidates).size())}));
            reader.flush();
            String i$ = CandidateListCompletionHandler.Messages.DISPLAY_CANDIDATES_NO.format(new Object[0]);
            String next = CandidateListCompletionHandler.Messages.DISPLAY_CANDIDATES_YES.format(new Object[0]);
            char[] allowed = new char[] { next.charAt(0), i$.charAt(0)};

            int copy;

            while ((copy = reader.readCharacter(allowed)) != -1) {
                String tmp = new String(new char[] { (char) copy});

                if (i$.startsWith(tmp)) {
                    reader.println();
                    return;
                }

                if (next.startsWith(tmp)) {
                    break;
                }

                reader.beep();
            }
        }

        if (distinct.size() != ((Collection) candidates).size()) {
            ArrayList copy1 = new ArrayList();
            Iterator i$1 = ((Collection) candidates).iterator();

            while (i$1.hasNext()) {
                CharSequence next1 = (CharSequence) i$1.next();

                if (!copy1.contains(next1)) {
                    copy1.add(next1);
                }
            }

            candidates = copy1;
        }

        reader.println();
        reader.printColumns((Collection) candidates);
    }

    private String getUnambiguousCompletions(List candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            String[] strings = (String[]) candidates.toArray(new String[candidates.size()]);
            String first = strings[0];
            StringBuilder candidate = new StringBuilder();

            for (int i = 0; i < first.length() && this.startsWith(first.substring(0, i + 1), strings); ++i) {
                candidate.append(first.charAt(i));
            }

            return candidate.toString();
        } else {
            return null;
        }
    }

    private boolean startsWith(String starts, String[] candidates) {
        String[] arr$ = candidates;
        int len$ = candidates.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            String candidate = arr$[i$];

            if (!candidate.startsWith(starts)) {
                return false;
            }
        }

        return true;
    }

    private static enum Messages {

        DISPLAY_CANDIDATES, DISPLAY_CANDIDATES_YES, DISPLAY_CANDIDATES_NO;

        private static final ResourceBundle bundle = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName(), Locale.getDefault());

        public String format(Object... args) {
            return CandidateListCompletionHandler.Messages.bundle == null ? "" : String.format(CandidateListCompletionHandler.Messages.bundle.getString(this.name()), args);
        }
    }
}
