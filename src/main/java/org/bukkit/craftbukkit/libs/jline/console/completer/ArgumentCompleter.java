package org.bukkit.craftbukkit.libs.jline.console.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.craftbukkit.libs.jline.internal.Log;
import org.bukkit.craftbukkit.libs.jline.internal.Preconditions;

public class ArgumentCompleter implements Completer {

    private final ArgumentCompleter.ArgumentDelimiter delimiter;
    private final List completers;
    private boolean strict;

    public ArgumentCompleter(ArgumentCompleter.ArgumentDelimiter delimiter, Collection completers) {
        this.completers = new ArrayList();
        this.strict = true;
        this.delimiter = (ArgumentCompleter.ArgumentDelimiter) Preconditions.checkNotNull(delimiter);
        Preconditions.checkNotNull(completers);
        this.completers.addAll(completers);
    }

    public ArgumentCompleter(ArgumentCompleter.ArgumentDelimiter delimiter, Completer... completers) {
        this(delimiter, (Collection) Arrays.asList(completers));
    }

    public ArgumentCompleter(Completer... completers) {
        this(new ArgumentCompleter.WhitespaceArgumentDelimiter(), completers);
    }

    public ArgumentCompleter(List completers) {
        this(new ArgumentCompleter.WhitespaceArgumentDelimiter(), (Collection) completers);
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isStrict() {
        return this.strict;
    }

    public ArgumentCompleter.ArgumentDelimiter getDelimiter() {
        return this.delimiter;
    }

    public List getCompleters() {
        return this.completers;
    }

    public int complete(String buffer, int cursor, List candidates) {
        Preconditions.checkNotNull(candidates);
        ArgumentCompleter.ArgumentDelimiter delim = this.getDelimiter();
        ArgumentCompleter.ArgumentList list = delim.delimit(buffer, cursor);
        int argpos = list.getArgumentPosition();
        int argIndex = list.getCursorArgumentIndex();

        if (argIndex < 0) {
            return -1;
        } else {
            List completers = this.getCompleters();
            Completer completer;

            if (argIndex >= completers.size()) {
                completer = (Completer) completers.get(completers.size() - 1);
            } else {
                completer = (Completer) completers.get(argIndex);
            }

            int ret;

            for (ret = 0; this.isStrict() && ret < argIndex; ++ret) {
                Completer pos = (Completer) completers.get(ret >= completers.size() ? completers.size() - 1 : ret);
                String[] i = list.getArguments();
                String val = i != null && ret < i.length ? i[ret] : "";
                LinkedList subCandidates = new LinkedList();

                if (pos.complete(val, val.length(), subCandidates) == -1) {
                    return -1;
                }

                if (subCandidates.size() == 0) {
                    return -1;
                }
            }

            ret = completer.complete(list.getCursorArgument(), argpos, candidates);
            if (ret == -1) {
                return -1;
            } else {
                int i = ret + list.getBufferPosition() - argpos;

                if (cursor != buffer.length() && delim.isDelimiter(buffer, cursor)) {
                    for (int j = 0; j < candidates.size(); ++j) {
                        CharSequence charsequence;

                        for (charsequence = (CharSequence) candidates.get(j); charsequence.length() > 0 && delim.isDelimiter(charsequence, charsequence.length() - 1); charsequence = charsequence.subSequence(0, charsequence.length() - 1)) {
                            ;
                        }

                        candidates.set(j, charsequence);
                    }
                }

                Log.trace(new Object[] { "Completing ", buffer, " (pos=", Integer.valueOf(cursor), ") with: ", candidates, ": offset=", Integer.valueOf(i)});
                return i;
            }
        }
    }

    public static class ArgumentList {

        private String[] arguments;
        private int cursorArgumentIndex;
        private int argumentPosition;
        private int bufferPosition;

        public ArgumentList(String[] arguments, int cursorArgumentIndex, int argumentPosition, int bufferPosition) {
            this.arguments = (String[]) Preconditions.checkNotNull(arguments);
            this.cursorArgumentIndex = cursorArgumentIndex;
            this.argumentPosition = argumentPosition;
            this.bufferPosition = bufferPosition;
        }

        public void setCursorArgumentIndex(int i) {
            this.cursorArgumentIndex = i;
        }

        public int getCursorArgumentIndex() {
            return this.cursorArgumentIndex;
        }

        public String getCursorArgument() {
            return this.cursorArgumentIndex >= 0 && this.cursorArgumentIndex < this.arguments.length ? this.arguments[this.cursorArgumentIndex] : null;
        }

        public void setArgumentPosition(int pos) {
            this.argumentPosition = pos;
        }

        public int getArgumentPosition() {
            return this.argumentPosition;
        }

        public void setArguments(String[] arguments) {
            this.arguments = arguments;
        }

        public String[] getArguments() {
            return this.arguments;
        }

        public void setBufferPosition(int pos) {
            this.bufferPosition = pos;
        }

        public int getBufferPosition() {
            return this.bufferPosition;
        }
    }

    public static class WhitespaceArgumentDelimiter extends ArgumentCompleter.AbstractArgumentDelimiter {

        public boolean isDelimiterChar(CharSequence buffer, int pos) {
            return Character.isWhitespace(buffer.charAt(pos));
        }
    }

    public abstract static class AbstractArgumentDelimiter implements ArgumentCompleter.ArgumentDelimiter {

        private char[] quoteChars = new char[] { '\'', '\"'};
        private char[] escapeChars = new char[] { '\\'};

        public void setQuoteChars(char[] chars) {
            this.quoteChars = chars;
        }

        public char[] getQuoteChars() {
            return this.quoteChars;
        }

        public void setEscapeChars(char[] chars) {
            this.escapeChars = chars;
        }

        public char[] getEscapeChars() {
            return this.escapeChars;
        }

        public ArgumentCompleter.ArgumentList delimit(CharSequence buffer, int cursor) {
            LinkedList args = new LinkedList();
            StringBuilder arg = new StringBuilder();
            int argpos = -1;
            int bindex = -1;
            int quoteStart = -1;

            for (int i = 0; buffer != null && i < buffer.length(); ++i) {
                if (i == cursor) {
                    bindex = args.size();
                    argpos = arg.length();
                }

                if (quoteStart < 0 && this.isQuoteChar(buffer, i)) {
                    quoteStart = i;
                } else if (quoteStart >= 0) {
                    if (buffer.charAt(quoteStart) == buffer.charAt(i) && !this.isEscaped(buffer, i)) {
                        args.add(arg.toString());
                        arg.setLength(0);
                        quoteStart = -1;
                    } else if (!this.isEscapeChar(buffer, i)) {
                        arg.append(buffer.charAt(i));
                    }
                } else if (this.isDelimiter(buffer, i)) {
                    if (arg.length() > 0) {
                        args.add(arg.toString());
                        arg.setLength(0);
                    }
                } else if (!this.isEscapeChar(buffer, i)) {
                    arg.append(buffer.charAt(i));
                }
            }

            if (cursor == buffer.length()) {
                bindex = args.size();
                argpos = arg.length();
            }

            if (arg.length() > 0) {
                args.add(arg.toString());
            }

            return new ArgumentCompleter.ArgumentList((String[]) args.toArray(new String[args.size()]), bindex, argpos, cursor);
        }

        public boolean isDelimiter(CharSequence buffer, int pos) {
            return !this.isQuoted(buffer, pos) && !this.isEscaped(buffer, pos) && this.isDelimiterChar(buffer, pos);
        }

        public boolean isQuoted(CharSequence buffer, int pos) {
            return false;
        }

        public boolean isQuoteChar(CharSequence buffer, int pos) {
            if (pos < 0) {
                return false;
            } else {
                for (int i = 0; this.quoteChars != null && i < this.quoteChars.length; ++i) {
                    if (buffer.charAt(pos) == this.quoteChars[i]) {
                        return !this.isEscaped(buffer, pos);
                    }
                }

                return false;
            }
        }

        public boolean isEscapeChar(CharSequence buffer, int pos) {
            if (pos < 0) {
                return false;
            } else {
                for (int i = 0; this.escapeChars != null && i < this.escapeChars.length; ++i) {
                    if (buffer.charAt(pos) == this.escapeChars[i]) {
                        return !this.isEscaped(buffer, pos);
                    }
                }

                return false;
            }
        }

        public boolean isEscaped(CharSequence buffer, int pos) {
            return pos <= 0 ? false : this.isEscapeChar(buffer, pos - 1);
        }

        public abstract boolean isDelimiterChar(CharSequence charsequence, int i);
    }

    public interface ArgumentDelimiter {

        ArgumentCompleter.ArgumentList delimit(CharSequence charsequence, int i);

        boolean isDelimiter(CharSequence charsequence, int i);
    }
}
