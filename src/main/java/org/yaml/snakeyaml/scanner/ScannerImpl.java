package org.yaml.snakeyaml.scanner;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.tokens.AliasToken;
import org.yaml.snakeyaml.tokens.AnchorToken;
import org.yaml.snakeyaml.tokens.BlockEndToken;
import org.yaml.snakeyaml.tokens.BlockEntryToken;
import org.yaml.snakeyaml.tokens.BlockMappingStartToken;
import org.yaml.snakeyaml.tokens.BlockSequenceStartToken;
import org.yaml.snakeyaml.tokens.DirectiveToken;
import org.yaml.snakeyaml.tokens.DocumentEndToken;
import org.yaml.snakeyaml.tokens.DocumentStartToken;
import org.yaml.snakeyaml.tokens.FlowEntryToken;
import org.yaml.snakeyaml.tokens.FlowMappingEndToken;
import org.yaml.snakeyaml.tokens.FlowMappingStartToken;
import org.yaml.snakeyaml.tokens.FlowSequenceEndToken;
import org.yaml.snakeyaml.tokens.FlowSequenceStartToken;
import org.yaml.snakeyaml.tokens.KeyToken;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.StreamEndToken;
import org.yaml.snakeyaml.tokens.StreamStartToken;
import org.yaml.snakeyaml.tokens.TagToken;
import org.yaml.snakeyaml.tokens.TagTuple;
import org.yaml.snakeyaml.tokens.Token;
import org.yaml.snakeyaml.tokens.ValueToken;
import org.yaml.snakeyaml.util.ArrayStack;
import org.yaml.snakeyaml.util.UriEncoder;

public final class ScannerImpl implements Scanner {

    private static final Pattern NOT_HEXA = Pattern.compile("[^0-9A-Fa-f]");
    public static final Map ESCAPE_REPLACEMENTS = new HashMap();
    public static final Map ESCAPE_CODES = new HashMap();
    private final StreamReader reader;
    private boolean done = false;
    private int flowLevel = 0;
    private List tokens;
    private int tokensTaken = 0;
    private int indent = -1;
    private ArrayStack indents;
    private boolean allowSimpleKey = true;
    private Map possibleSimpleKeys;

    public ScannerImpl(StreamReader reader) {
        this.reader = reader;
        this.tokens = new ArrayList(100);
        this.indents = new ArrayStack(10);
        this.possibleSimpleKeys = new LinkedHashMap();
        this.fetchStreamStart();
    }

    public boolean checkToken(Token.ID... choices) {
        while (this.needMoreTokens()) {
            this.fetchMoreTokens();
        }

        if (!this.tokens.isEmpty()) {
            if (choices.length == 0) {
                return true;
            }

            Token.ID first = ((Token) this.tokens.get(0)).getTokenId();

            for (int i = 0; i < choices.length; ++i) {
                if (first == choices[i]) {
                    return true;
                }
            }
        }

        return false;
    }

    public Token peekToken() {
        while (this.needMoreTokens()) {
            this.fetchMoreTokens();
        }

        return (Token) this.tokens.get(0);
    }

    public Token getToken() {
        if (!this.tokens.isEmpty()) {
            ++this.tokensTaken;
            return (Token) this.tokens.remove(0);
        } else {
            return null;
        }
    }

    private boolean needMoreTokens() {
        if (this.done) {
            return false;
        } else if (this.tokens.isEmpty()) {
            return true;
        } else {
            this.stalePossibleSimpleKeys();
            return this.nextPossibleSimpleKey() == this.tokensTaken;
        }
    }

    private void fetchMoreTokens() {
        this.scanToNextToken();
        this.stalePossibleSimpleKeys();
        this.unwindIndent(this.reader.getColumn());
        char ch = this.reader.peek();

        switch (ch) {
        case '\u0000':
            this.fetchStreamEnd();
            return;

        case '!':
            this.fetchTag();
            return;

        case '\"':
            this.fetchDouble();
            return;

        case '%':
            if (this.checkDirective()) {
                this.fetchDirective();
                return;
            }
            break;

        case '&':
            this.fetchAnchor();
            return;

        case '\'':
            this.fetchSingle();
            return;

        case '*':
            this.fetchAlias();
            return;

        case ',':
            this.fetchFlowEntry();
            return;

        case '-':
            if (this.checkDocumentStart()) {
                this.fetchDocumentStart();
                return;
            }

            if (this.checkBlockEntry()) {
                this.fetchBlockEntry();
                return;
            }
            break;

        case '.':
            if (this.checkDocumentEnd()) {
                this.fetchDocumentEnd();
                return;
            }
            break;

        case ':':
            if (this.checkValue()) {
                this.fetchValue();
                return;
            }
            break;

        case '>':
            if (this.flowLevel == 0) {
                this.fetchFolded();
                return;
            }
            break;

        case '?':
            if (this.checkKey()) {
                this.fetchKey();
                return;
            }
            break;

        case '[':
            this.fetchFlowSequenceStart();
            return;

        case ']':
            this.fetchFlowSequenceEnd();
            return;

        case '{':
            this.fetchFlowMappingStart();
            return;

        case '|':
            if (this.flowLevel == 0) {
                this.fetchLiteral();
                return;
            }
            break;

        case '}':
            this.fetchFlowMappingEnd();
            return;
        }

        if (this.checkPlain()) {
            this.fetchPlain();
        } else {
            String chRepresentation = String.valueOf(ch);
            Iterator text = ScannerImpl.ESCAPE_REPLACEMENTS.keySet().iterator();

            while (text.hasNext()) {
                Character s = (Character) text.next();
                String v = (String) ScannerImpl.ESCAPE_REPLACEMENTS.get(s);

                if (v.equals(chRepresentation)) {
                    chRepresentation = "\\" + s;
                    break;
                }
            }

            if (ch == 9) {
                chRepresentation = chRepresentation + "(TAB)";
            }

            String text1 = String.format("found character %s \'%s\' that cannot start any token. (Do not use %s for indentation)", new Object[] { Character.valueOf(ch), chRepresentation, chRepresentation});

            throw new ScannerException("while scanning for the next token", (Mark) null, text1, this.reader.getMark());
        }
    }

    private int nextPossibleSimpleKey() {
        return !this.possibleSimpleKeys.isEmpty() ? ((SimpleKey) this.possibleSimpleKeys.values().iterator().next()).getTokenNumber() : -1;
    }

    private void stalePossibleSimpleKeys() {
        if (!this.possibleSimpleKeys.isEmpty()) {
            Iterator iterator = this.possibleSimpleKeys.values().iterator();

            while (iterator.hasNext()) {
                SimpleKey key = (SimpleKey) iterator.next();

                if (key.getLine() != this.reader.getLine() || this.reader.getIndex() - key.getIndex() > 1024) {
                    if (key.isRequired()) {
                        throw new ScannerException("while scanning a simple key", key.getMark(), "could not find expected \':\'", this.reader.getMark());
                    }

                    iterator.remove();
                }
            }
        }

    }

    private void savePossibleSimpleKey() {
        boolean required = this.flowLevel == 0 && this.indent == this.reader.getColumn();

        if (!this.allowSimpleKey && required) {
            throw new YAMLException("A simple key is required only if it is the first token in the current line");
        } else {
            if (this.allowSimpleKey) {
                this.removePossibleSimpleKey();
                int tokenNumber = this.tokensTaken + this.tokens.size();
                SimpleKey key = new SimpleKey(tokenNumber, required, this.reader.getIndex(), this.reader.getLine(), this.reader.getColumn(), this.reader.getMark());

                this.possibleSimpleKeys.put(Integer.valueOf(this.flowLevel), key);
            }

        }
    }

    private void removePossibleSimpleKey() {
        SimpleKey key = (SimpleKey) this.possibleSimpleKeys.remove(Integer.valueOf(this.flowLevel));

        if (key != null && key.isRequired()) {
            throw new ScannerException("while scanning a simple key", key.getMark(), "could not find expected \':\'", this.reader.getMark());
        }
    }

    private void unwindIndent(int col) {
        if (this.flowLevel == 0) {
            while (this.indent > col) {
                Mark mark = this.reader.getMark();

                this.indent = ((Integer) this.indents.pop()).intValue();
                this.tokens.add(new BlockEndToken(mark, mark));
            }

        }
    }

    private boolean addIndent(int column) {
        if (this.indent < column) {
            this.indents.push(Integer.valueOf(this.indent));
            this.indent = column;
            return true;
        } else {
            return false;
        }
    }

    private void fetchStreamStart() {
        Mark mark = this.reader.getMark();
        StreamStartToken token = new StreamStartToken(mark, mark);

        this.tokens.add(token);
    }

    private void fetchStreamEnd() {
        this.unwindIndent(-1);
        this.removePossibleSimpleKey();
        this.allowSimpleKey = false;
        this.possibleSimpleKeys.clear();
        Mark mark = this.reader.getMark();
        StreamEndToken token = new StreamEndToken(mark, mark);

        this.tokens.add(token);
        this.done = true;
    }

    private void fetchDirective() {
        this.unwindIndent(-1);
        this.removePossibleSimpleKey();
        this.allowSimpleKey = false;
        Token tok = this.scanDirective();

        this.tokens.add(tok);
    }

    private void fetchDocumentStart() {
        this.fetchDocumentIndicator(true);
    }

    private void fetchDocumentEnd() {
        this.fetchDocumentIndicator(false);
    }

    private void fetchDocumentIndicator(boolean isDocumentStart) {
        this.unwindIndent(-1);
        this.removePossibleSimpleKey();
        this.allowSimpleKey = false;
        Mark startMark = this.reader.getMark();

        this.reader.forward(3);
        Mark endMark = this.reader.getMark();
        Object token;

        if (isDocumentStart) {
            token = new DocumentStartToken(startMark, endMark);
        } else {
            token = new DocumentEndToken(startMark, endMark);
        }

        this.tokens.add(token);
    }

    private void fetchFlowSequenceStart() {
        this.fetchFlowCollectionStart(false);
    }

    private void fetchFlowMappingStart() {
        this.fetchFlowCollectionStart(true);
    }

    private void fetchFlowCollectionStart(boolean isMappingStart) {
        this.savePossibleSimpleKey();
        ++this.flowLevel;
        this.allowSimpleKey = true;
        Mark startMark = this.reader.getMark();

        this.reader.forward(1);
        Mark endMark = this.reader.getMark();
        Object token;

        if (isMappingStart) {
            token = new FlowMappingStartToken(startMark, endMark);
        } else {
            token = new FlowSequenceStartToken(startMark, endMark);
        }

        this.tokens.add(token);
    }

    private void fetchFlowSequenceEnd() {
        this.fetchFlowCollectionEnd(false);
    }

    private void fetchFlowMappingEnd() {
        this.fetchFlowCollectionEnd(true);
    }

    private void fetchFlowCollectionEnd(boolean isMappingEnd) {
        this.removePossibleSimpleKey();
        --this.flowLevel;
        this.allowSimpleKey = false;
        Mark startMark = this.reader.getMark();

        this.reader.forward();
        Mark endMark = this.reader.getMark();
        Object token;

        if (isMappingEnd) {
            token = new FlowMappingEndToken(startMark, endMark);
        } else {
            token = new FlowSequenceEndToken(startMark, endMark);
        }

        this.tokens.add(token);
    }

    private void fetchFlowEntry() {
        this.allowSimpleKey = true;
        this.removePossibleSimpleKey();
        Mark startMark = this.reader.getMark();

        this.reader.forward();
        Mark endMark = this.reader.getMark();
        FlowEntryToken token = new FlowEntryToken(startMark, endMark);

        this.tokens.add(token);
    }

    private void fetchBlockEntry() {
        Mark startMark;

        if (this.flowLevel == 0) {
            if (!this.allowSimpleKey) {
                throw new ScannerException((String) null, (Mark) null, "sequence entries are not allowed here", this.reader.getMark());
            }

            if (this.addIndent(this.reader.getColumn())) {
                startMark = this.reader.getMark();
                this.tokens.add(new BlockSequenceStartToken(startMark, startMark));
            }
        }

        this.allowSimpleKey = true;
        this.removePossibleSimpleKey();
        startMark = this.reader.getMark();
        this.reader.forward();
        Mark endMark = this.reader.getMark();
        BlockEntryToken token = new BlockEntryToken(startMark, endMark);

        this.tokens.add(token);
    }

    private void fetchKey() {
        Mark startMark;

        if (this.flowLevel == 0) {
            if (!this.allowSimpleKey) {
                throw new ScannerException((String) null, (Mark) null, "mapping keys are not allowed here", this.reader.getMark());
            }

            if (this.addIndent(this.reader.getColumn())) {
                startMark = this.reader.getMark();
                this.tokens.add(new BlockMappingStartToken(startMark, startMark));
            }
        }

        this.allowSimpleKey = this.flowLevel == 0;
        this.removePossibleSimpleKey();
        startMark = this.reader.getMark();
        this.reader.forward();
        Mark endMark = this.reader.getMark();
        KeyToken token = new KeyToken(startMark, endMark);

        this.tokens.add(token);
    }

    private void fetchValue() {
        SimpleKey key = (SimpleKey) this.possibleSimpleKeys.remove(Integer.valueOf(this.flowLevel));
        Mark startMark;

        if (key != null) {
            this.tokens.add(key.getTokenNumber() - this.tokensTaken, new KeyToken(key.getMark(), key.getMark()));
            if (this.flowLevel == 0 && this.addIndent(key.getColumn())) {
                this.tokens.add(key.getTokenNumber() - this.tokensTaken, new BlockMappingStartToken(key.getMark(), key.getMark()));
            }

            this.allowSimpleKey = false;
        } else {
            if (this.flowLevel == 0 && !this.allowSimpleKey) {
                throw new ScannerException((String) null, (Mark) null, "mapping values are not allowed here", this.reader.getMark());
            }

            if (this.flowLevel == 0 && this.addIndent(this.reader.getColumn())) {
                startMark = this.reader.getMark();
                this.tokens.add(new BlockMappingStartToken(startMark, startMark));
            }

            this.allowSimpleKey = this.flowLevel == 0;
            this.removePossibleSimpleKey();
        }

        startMark = this.reader.getMark();
        this.reader.forward();
        Mark endMark = this.reader.getMark();
        ValueToken token = new ValueToken(startMark, endMark);

        this.tokens.add(token);
    }

    private void fetchAlias() {
        this.savePossibleSimpleKey();
        this.allowSimpleKey = false;
        Token tok = this.scanAnchor(false);

        this.tokens.add(tok);
    }

    private void fetchAnchor() {
        this.savePossibleSimpleKey();
        this.allowSimpleKey = false;
        Token tok = this.scanAnchor(true);

        this.tokens.add(tok);
    }

    private void fetchTag() {
        this.savePossibleSimpleKey();
        this.allowSimpleKey = false;
        Token tok = this.scanTag();

        this.tokens.add(tok);
    }

    private void fetchLiteral() {
        this.fetchBlockScalar('|');
    }

    private void fetchFolded() {
        this.fetchBlockScalar('>');
    }

    private void fetchBlockScalar(char style) {
        this.allowSimpleKey = true;
        this.removePossibleSimpleKey();
        Token tok = this.scanBlockScalar(style);

        this.tokens.add(tok);
    }

    private void fetchSingle() {
        this.fetchFlowScalar('\'');
    }

    private void fetchDouble() {
        this.fetchFlowScalar('\"');
    }

    private void fetchFlowScalar(char style) {
        this.savePossibleSimpleKey();
        this.allowSimpleKey = false;
        Token tok = this.scanFlowScalar(style);

        this.tokens.add(tok);
    }

    private void fetchPlain() {
        this.savePossibleSimpleKey();
        this.allowSimpleKey = false;
        Token tok = this.scanPlain();

        this.tokens.add(tok);
    }

    private boolean checkDirective() {
        return this.reader.getColumn() == 0;
    }

    private boolean checkDocumentStart() {
        return this.reader.getColumn() == 0 && "---".equals(this.reader.prefix(3)) && Constant.NULL_BL_T_LINEBR.has(this.reader.peek(3));
    }

    private boolean checkDocumentEnd() {
        return this.reader.getColumn() == 0 && "...".equals(this.reader.prefix(3)) && Constant.NULL_BL_T_LINEBR.has(this.reader.peek(3));
    }

    private boolean checkBlockEntry() {
        return Constant.NULL_BL_T_LINEBR.has(this.reader.peek(1));
    }

    private boolean checkKey() {
        return this.flowLevel != 0 ? true : Constant.NULL_BL_T_LINEBR.has(this.reader.peek(1));
    }

    private boolean checkValue() {
        return this.flowLevel != 0 ? true : Constant.NULL_BL_T_LINEBR.has(this.reader.peek(1));
    }

    private boolean checkPlain() {
        char ch = this.reader.peek();

        return Constant.NULL_BL_T_LINEBR.hasNo(ch, "-?:,[]{}#&*!|>\'\"%@`") || Constant.NULL_BL_T_LINEBR.hasNo(this.reader.peek(1)) && (ch == 45 || this.flowLevel == 0 && "?:".indexOf(ch) != -1);
    }

    private void scanToNextToken() {
        if (this.reader.getIndex() == 0 && this.reader.peek() == '\ufeff') {
            this.reader.forward();
        }

        boolean found = false;

        while (!found) {
            int ff;

            for (ff = 0; this.reader.peek(ff) == 32; ++ff) {
                ;
            }

            if (ff > 0) {
                this.reader.forward(ff);
            }

            if (this.reader.peek() == 35) {
                for (ff = 0; Constant.NULL_OR_LINEBR.hasNo(this.reader.peek(ff)); ++ff) {
                    ;
                }

                if (ff > 0) {
                    this.reader.forward(ff);
                }
            }

            if (this.scanLineBreak().length() != 0) {
                if (this.flowLevel == 0) {
                    this.allowSimpleKey = true;
                }
            } else {
                found = true;
            }
        }

    }

    private Token scanDirective() {
        Mark startMark = this.reader.getMark();

        this.reader.forward();
        String name = this.scanDirectiveName(startMark);
        List value = null;
        Mark endMark;

        if ("YAML".equals(name)) {
            value = this.scanYamlDirectiveValue(startMark);
            endMark = this.reader.getMark();
        } else if ("TAG".equals(name)) {
            value = this.scanTagDirectiveValue(startMark);
            endMark = this.reader.getMark();
        } else {
            endMark = this.reader.getMark();

            int ff;

            for (ff = 0; Constant.NULL_OR_LINEBR.hasNo(this.reader.peek(ff)); ++ff) {
                ;
            }

            if (ff > 0) {
                this.reader.forward(ff);
            }
        }

        this.scanDirectiveIgnoredLine(startMark);
        return new DirectiveToken(name, value, startMark, endMark);
    }

    private String scanDirectiveName(Mark startMark) {
        int length = 0;

        char ch;

        for (ch = this.reader.peek(length); Constant.ALPHA.has(ch); ch = this.reader.peek(length)) {
            ++length;
        }

        if (length == 0) {
            throw new ScannerException("while scanning a directive", startMark, "expected alphabetic or numeric character, but found " + ch + "(" + ch + ")", this.reader.getMark());
        } else {
            String value = this.reader.prefixForward(length);

            ch = this.reader.peek();
            if (Constant.NULL_BL_LINEBR.hasNo(ch)) {
                throw new ScannerException("while scanning a directive", startMark, "expected alphabetic or numeric character, but found " + ch + "(" + ch + ")", this.reader.getMark());
            } else {
                return value;
            }
        }
    }

    private List scanYamlDirectiveValue(Mark startMark) {
        while (this.reader.peek() == 32) {
            this.reader.forward();
        }

        Integer major = this.scanYamlDirectiveNumber(startMark);

        if (this.reader.peek() != 46) {
            throw new ScannerException("while scanning a directive", startMark, "expected a digit or \'.\', but found " + this.reader.peek() + "(" + this.reader.peek() + ")", this.reader.getMark());
        } else {
            this.reader.forward();
            Integer minor = this.scanYamlDirectiveNumber(startMark);

            if (Constant.NULL_BL_LINEBR.hasNo(this.reader.peek())) {
                throw new ScannerException("while scanning a directive", startMark, "expected a digit or \' \', but found " + this.reader.peek() + "(" + this.reader.peek() + ")", this.reader.getMark());
            } else {
                ArrayList result = new ArrayList(2);

                result.add(major);
                result.add(minor);
                return result;
            }
        }
    }

    private Integer scanYamlDirectiveNumber(Mark startMark) {
        char ch = this.reader.peek();

        if (!Character.isDigit(ch)) {
            throw new ScannerException("while scanning a directive", startMark, "expected a digit, but found " + ch + "(" + ch + ")", this.reader.getMark());
        } else {
            int length;

            for (length = 0; Character.isDigit(this.reader.peek(length)); ++length) {
                ;
            }

            Integer value = Integer.valueOf(Integer.parseInt(this.reader.prefixForward(length)));

            return value;
        }
    }

    private List scanTagDirectiveValue(Mark startMark) {
        while (this.reader.peek() == 32) {
            this.reader.forward();
        }

        String handle = this.scanTagDirectiveHandle(startMark);

        while (this.reader.peek() == 32) {
            this.reader.forward();
        }

        String prefix = this.scanTagDirectivePrefix(startMark);
        ArrayList result = new ArrayList(2);

        result.add(handle);
        result.add(prefix);
        return result;
    }

    private String scanTagDirectiveHandle(Mark startMark) {
        String value = this.scanTagHandle("directive", startMark);
        char ch = this.reader.peek();

        if (ch != 32) {
            throw new ScannerException("while scanning a directive", startMark, "expected \' \', but found " + this.reader.peek() + "(" + ch + ")", this.reader.getMark());
        } else {
            return value;
        }
    }

    private String scanTagDirectivePrefix(Mark startMark) {
        String value = this.scanTagUri("directive", startMark);

        if (Constant.NULL_BL_LINEBR.hasNo(this.reader.peek())) {
            throw new ScannerException("while scanning a directive", startMark, "expected \' \', but found " + this.reader.peek() + "(" + this.reader.peek() + ")", this.reader.getMark());
        } else {
            return value;
        }
    }

    private String scanDirectiveIgnoredLine(Mark startMark) {
        int ff;

        for (ff = 0; this.reader.peek(ff) == 32; ++ff) {
            ;
        }

        if (ff > 0) {
            this.reader.forward(ff);
        }

        if (this.reader.peek() == 35) {
            for (ff = 0; Constant.NULL_OR_LINEBR.hasNo(this.reader.peek(ff)); ++ff) {
                ;
            }

            this.reader.forward(ff);
        }

        char ch = this.reader.peek();
        String lineBreak = this.scanLineBreak();

        if (lineBreak.length() == 0 && ch != 0) {
            throw new ScannerException("while scanning a directive", startMark, "expected a comment or a line break, but found " + ch + "(" + ch + ")", this.reader.getMark());
        } else {
            return lineBreak;
        }
    }

    private Token scanAnchor(boolean isAnchor) {
        Mark startMark = this.reader.getMark();
        char indicator = this.reader.peek();
        String name = indicator == 42 ? "alias" : "anchor";

        this.reader.forward();
        int length = 0;

        char ch;

        for (ch = this.reader.peek(length); Constant.ALPHA.has(ch); ch = this.reader.peek(length)) {
            ++length;
        }

        if (length == 0) {
            throw new ScannerException("while scanning an " + name, startMark, "expected alphabetic or numeric character, but found but found " + ch, this.reader.getMark());
        } else {
            String value = this.reader.prefixForward(length);

            ch = this.reader.peek();
            if (Constant.NULL_BL_T_LINEBR.hasNo(ch, "?:,]}%@`")) {
                throw new ScannerException("while scanning an " + name, startMark, "expected alphabetic or numeric character, but found " + ch + "(" + this.reader.peek() + ")", this.reader.getMark());
            } else {
                Mark endMark = this.reader.getMark();
                Object tok;

                if (isAnchor) {
                    tok = new AnchorToken(value, startMark, endMark);
                } else {
                    tok = new AliasToken(value, startMark, endMark);
                }

                return (Token) tok;
            }
        }
    }

    private Token scanTag() {
        Mark startMark = this.reader.getMark();
        char ch = this.reader.peek(1);
        String handle = null;
        String suffix = null;

        if (ch == 60) {
            this.reader.forward(2);
            suffix = this.scanTagUri("tag", startMark);
            if (this.reader.peek() != 62) {
                throw new ScannerException("while scanning a tag", startMark, "expected \'>\', but found \'" + this.reader.peek() + "\' (" + this.reader.peek() + ")", this.reader.getMark());
            }

            this.reader.forward();
        } else if (Constant.NULL_BL_T_LINEBR.has(ch)) {
            suffix = "!";
            this.reader.forward();
        } else {
            int value = 1;

            boolean endMark;

            for (endMark = false; Constant.NULL_BL_LINEBR.hasNo(ch); ch = this.reader.peek(value)) {
                if (ch == 33) {
                    endMark = true;
                    break;
                }

                ++value;
            }

            handle = "!";
            if (endMark) {
                handle = this.scanTagHandle("tag", startMark);
            } else {
                handle = "!";
                this.reader.forward();
            }

            suffix = this.scanTagUri("tag", startMark);
        }

        ch = this.reader.peek();
        if (Constant.NULL_BL_LINEBR.hasNo(ch)) {
            throw new ScannerException("while scanning a tag", startMark, "expected \' \', but found \'" + ch + "\' (" + ch + ")", this.reader.getMark());
        } else {
            TagTuple tagtuple = new TagTuple(handle, suffix);
            Mark mark = this.reader.getMark();

            return new TagToken(tagtuple, startMark, mark);
        }
    }

    private Token scanBlockScalar(char style) {
        boolean folded;

        if (style == 62) {
            folded = true;
        } else {
            folded = false;
        }

        StringBuilder chunks = new StringBuilder();
        Mark startMark = this.reader.getMark();

        this.reader.forward();
        ScannerImpl.Chomping chompi = this.scanBlockScalarIndicators(startMark);
        int increment = chompi.getIncrement();

        this.scanBlockScalarIgnoredLine(startMark);
        int minIndent = this.indent + 1;

        if (minIndent < 1) {
            minIndent = 1;
        }

        String breaks = null;
        boolean maxIndent = false;
        boolean indent = false;
        Object[] lineBreak;
        Mark endMark;
        int i;

        if (increment == -1) {
            lineBreak = this.scanBlockScalarIndentation();
            breaks = (String) lineBreak[0];
            int j = ((Integer) lineBreak[1]).intValue();

            endMark = (Mark) lineBreak[2];
            i = Math.max(minIndent, j);
        } else {
            i = minIndent + increment - 1;
            lineBreak = this.scanBlockScalarBreaks(i);
            breaks = (String) lineBreak[0];
            endMark = (Mark) lineBreak[1];
        }

        String s = "";

        while (this.reader.getColumn() == i && this.reader.peek() != 0) {
            chunks.append(breaks);
            boolean leadingNonSpace = " \t".indexOf(this.reader.peek()) == -1;

            int length;

            for (length = 0; Constant.NULL_OR_LINEBR.hasNo(this.reader.peek(length)); ++length) {
                ;
            }

            chunks.append(this.reader.prefixForward(length));
            s = this.scanLineBreak();
            Object[] brme = this.scanBlockScalarBreaks(i);

            breaks = (String) brme[0];
            endMark = (Mark) brme[1];
            if (this.reader.getColumn() != i || this.reader.peek() == 0) {
                break;
            }

            if (folded && "\n".equals(s) && leadingNonSpace && " \t".indexOf(this.reader.peek()) == -1) {
                if (breaks.length() == 0) {
                    chunks.append(" ");
                }
            } else {
                chunks.append(s);
            }
        }

        if (chompi.chompTailIsNotFalse()) {
            chunks.append(s);
        }

        if (chompi.chompTailIsTrue()) {
            chunks.append(breaks);
        }

        return new ScalarToken(chunks.toString(), false, startMark, endMark, style);
    }

    private ScannerImpl.Chomping scanBlockScalarIndicators(Mark startMark) {
        Boolean chomping = null;
        int increment = -1;
        char ch = this.reader.peek();

        if (ch != 45 && ch != 43) {
            if (Character.isDigit(ch)) {
                increment = Integer.parseInt(String.valueOf(ch));
                if (increment == 0) {
                    throw new ScannerException("while scanning a block scalar", startMark, "expected indentation indicator in the range 1-9, but found 0", this.reader.getMark());
                }

                this.reader.forward();
                ch = this.reader.peek();
                if (ch == 45 || ch == 43) {
                    if (ch == 43) {
                        chomping = Boolean.TRUE;
                    } else {
                        chomping = Boolean.FALSE;
                    }

                    this.reader.forward();
                }
            }
        } else {
            if (ch == 43) {
                chomping = Boolean.TRUE;
            } else {
                chomping = Boolean.FALSE;
            }

            this.reader.forward();
            ch = this.reader.peek();
            if (Character.isDigit(ch)) {
                increment = Integer.parseInt(String.valueOf(ch));
                if (increment == 0) {
                    throw new ScannerException("while scanning a block scalar", startMark, "expected indentation indicator in the range 1-9, but found 0", this.reader.getMark());
                }

                this.reader.forward();
            }
        }

        ch = this.reader.peek();
        if (Constant.NULL_BL_LINEBR.hasNo(ch)) {
            throw new ScannerException("while scanning a block scalar", startMark, "expected chomping or indentation indicators, but found " + ch, this.reader.getMark());
        } else {
            return new ScannerImpl.Chomping(chomping, increment);
        }
    }

    private String scanBlockScalarIgnoredLine(Mark startMark) {
        int ff;

        for (ff = 0; this.reader.peek(ff) == 32; ++ff) {
            ;
        }

        if (ff > 0) {
            this.reader.forward(ff);
        }

        if (this.reader.peek() == 35) {
            for (ff = 0; Constant.NULL_OR_LINEBR.hasNo(this.reader.peek(ff)); ++ff) {
                ;
            }

            if (ff > 0) {
                this.reader.forward(ff);
            }
        }

        char ch = this.reader.peek();
        String lineBreak = this.scanLineBreak();

        if (lineBreak.length() == 0 && ch != 0) {
            throw new ScannerException("while scanning a block scalar", startMark, "expected a comment or a line break, but found " + ch, this.reader.getMark());
        } else {
            return lineBreak;
        }
    }

    private Object[] scanBlockScalarIndentation() {
        StringBuilder chunks = new StringBuilder();
        int maxIndent = 0;
        Mark endMark = this.reader.getMark();

        while (Constant.LINEBR.has(this.reader.peek(), " \r")) {
            if (this.reader.peek() != 32) {
                chunks.append(this.scanLineBreak());
                endMark = this.reader.getMark();
            } else {
                this.reader.forward();
                if (this.reader.getColumn() > maxIndent) {
                    maxIndent = this.reader.getColumn();
                }
            }
        }

        return new Object[] { chunks.toString(), Integer.valueOf(maxIndent), endMark};
    }

    private Object[] scanBlockScalarBreaks(int indent) {
        StringBuilder chunks = new StringBuilder();
        Mark endMark = this.reader.getMark();
        int ff = 0;

        int col;

        for (col = this.reader.getColumn(); col < indent && this.reader.peek(ff) == 32; ++col) {
            ++ff;
        }

        if (ff > 0) {
            this.reader.forward(ff);
        }

        String lineBreak = null;

        while ((lineBreak = this.scanLineBreak()).length() != 0) {
            chunks.append(lineBreak);
            endMark = this.reader.getMark();
            ff = 0;

            for (col = this.reader.getColumn(); col < indent && this.reader.peek(ff) == 32; ++col) {
                ++ff;
            }

            if (ff > 0) {
                this.reader.forward(ff);
            }
        }

        return new Object[] { chunks.toString(), endMark};
    }

    private Token scanFlowScalar(char style) {
        boolean _double;

        if (style == 34) {
            _double = true;
        } else {
            _double = false;
        }

        StringBuilder chunks = new StringBuilder();
        Mark startMark = this.reader.getMark();
        char quote = this.reader.peek();

        this.reader.forward();
        chunks.append(this.scanFlowScalarNonSpaces(_double, startMark));

        while (this.reader.peek() != quote) {
            chunks.append(this.scanFlowScalarSpaces(startMark));
            chunks.append(this.scanFlowScalarNonSpaces(_double, startMark));
        }

        this.reader.forward();
        Mark endMark = this.reader.getMark();

        return new ScalarToken(chunks.toString(), false, startMark, endMark, style);
    }

    private String scanFlowScalarNonSpaces(boolean doubleQuoted, Mark startMark) {
        StringBuilder chunks = new StringBuilder();

        while (true) {
            while (true) {
                while (true) {
                    int length;

                    for (length = 0; Constant.NULL_BL_T_LINEBR.hasNo(this.reader.peek(length), "\'\"\\"); ++length) {
                        ;
                    }

                    if (length != 0) {
                        chunks.append(this.reader.prefixForward(length));
                    }

                    char ch = this.reader.peek();

                    if (doubleQuoted || ch != 39 || this.reader.peek(1) != 39) {
                        if ((!doubleQuoted || ch != 39) && (doubleQuoted || "\"\\".indexOf(ch) == -1)) {
                            if (!doubleQuoted || ch != 92) {
                                return chunks.toString();
                            }

                            this.reader.forward();
                            ch = this.reader.peek();
                            if (ScannerImpl.ESCAPE_REPLACEMENTS.containsKey(Character.valueOf(ch))) {
                                chunks.append((String) ScannerImpl.ESCAPE_REPLACEMENTS.get(Character.valueOf(ch)));
                                this.reader.forward();
                            } else if (ScannerImpl.ESCAPE_CODES.containsKey(Character.valueOf(ch))) {
                                length = ((Integer) ScannerImpl.ESCAPE_CODES.get(Character.valueOf(ch))).intValue();
                                this.reader.forward();
                                String hex = this.reader.prefix(length);

                                if (ScannerImpl.NOT_HEXA.matcher(hex).find()) {
                                    throw new ScannerException("while scanning a double-quoted scalar", startMark, "expected escape sequence of " + length + " hexadecimal numbers, but found: " + hex, this.reader.getMark());
                                }

                                int decimal = Integer.parseInt(hex, 16);
                                String unicode = new String(Character.toChars(decimal));

                                chunks.append(unicode);
                                this.reader.forward(length);
                            } else {
                                if (this.scanLineBreak().length() == 0) {
                                    throw new ScannerException("while scanning a double-quoted scalar", startMark, "found unknown escape character " + ch + "(" + ch + ")", this.reader.getMark());
                                }

                                chunks.append(this.scanFlowScalarBreaks(startMark));
                            }
                        } else {
                            chunks.append(ch);
                            this.reader.forward();
                        }
                    } else {
                        chunks.append("\'");
                        this.reader.forward(2);
                    }
                }
            }
        }
    }

    private String scanFlowScalarSpaces(Mark startMark) {
        StringBuilder chunks = new StringBuilder();

        int length;

        for (length = 0; " \t".indexOf(this.reader.peek(length)) != -1; ++length) {
            ;
        }

        String whitespaces = this.reader.prefixForward(length);
        char ch = this.reader.peek();

        if (ch == 0) {
            throw new ScannerException("while scanning a quoted scalar", startMark, "found unexpected end of stream", this.reader.getMark());
        } else {
            String lineBreak = this.scanLineBreak();

            if (lineBreak.length() != 0) {
                String breaks = this.scanFlowScalarBreaks(startMark);

                if (!"\n".equals(lineBreak)) {
                    chunks.append(lineBreak);
                } else if (breaks.length() == 0) {
                    chunks.append(" ");
                }

                chunks.append(breaks);
            } else {
                chunks.append(whitespaces);
            }

            return chunks.toString();
        }
    }

    private String scanFlowScalarBreaks(Mark startMark) {
        StringBuilder chunks = new StringBuilder();

        while (true) {
            String prefix = this.reader.prefix(3);

            if (("---".equals(prefix) || "...".equals(prefix)) && Constant.NULL_BL_T_LINEBR.has(this.reader.peek(3))) {
                throw new ScannerException("while scanning a quoted scalar", startMark, "found unexpected document separator", this.reader.getMark());
            }

            while (" \t".indexOf(this.reader.peek()) != -1) {
                this.reader.forward();
            }

            String lineBreak = this.scanLineBreak();

            if (lineBreak.length() == 0) {
                return chunks.toString();
            }

            chunks.append(lineBreak);
        }
    }

    private Token scanPlain() {
        StringBuilder chunks = new StringBuilder();
        Mark startMark = this.reader.getMark();
        Mark endMark = startMark;
        int indent = this.indent + 1;
        String spaces = "";

        do {
            int length = 0;

            if (this.reader.peek() == 35) {
                break;
            }

            while (true) {
                char ch = this.reader.peek(length);

                if (Constant.NULL_BL_T_LINEBR.has(ch) || this.flowLevel == 0 && ch == 58 && Constant.NULL_BL_T_LINEBR.has(this.reader.peek(length + 1)) || this.flowLevel != 0 && ",:?[]{}".indexOf(ch) != -1) {
                    if (this.flowLevel != 0 && ch == 58 && Constant.NULL_BL_T_LINEBR.hasNo(this.reader.peek(length + 1), ",[]{}")) {
                        this.reader.forward(length);
                        throw new ScannerException("while scanning a plain scalar", startMark, "found unexpected \':\'", this.reader.getMark(), "Please check http://pyyaml.org/wiki/YAMLColonInFlowContext for details.");
                    }

                    if (length == 0) {
                        return new ScalarToken(chunks.toString(), startMark, endMark, true);
                    }

                    this.allowSimpleKey = false;
                    chunks.append(spaces);
                    chunks.append(this.reader.prefixForward(length));
                    endMark = this.reader.getMark();
                    spaces = this.scanPlainSpaces();
                    break;
                }

                ++length;
            }
        } while (spaces.length() != 0 && this.reader.peek() != 35 && (this.flowLevel != 0 || this.reader.getColumn() >= indent));

        return new ScalarToken(chunks.toString(), startMark, endMark, true);
    }

    private String scanPlainSpaces() {
        int length;

        for (length = 0; this.reader.peek(length) == 32 || this.reader.peek(length) == 9; ++length) {
            ;
        }

        String whitespaces = this.reader.prefixForward(length);
        String lineBreak = this.scanLineBreak();

        if (lineBreak.length() == 0) {
            return whitespaces;
        } else {
            this.allowSimpleKey = true;
            String prefix = this.reader.prefix(3);

            if (!"---".equals(prefix) && (!"...".equals(prefix) || !Constant.NULL_BL_T_LINEBR.has(this.reader.peek(3)))) {
                StringBuilder breaks = new StringBuilder();

                do {
                    while (this.reader.peek() == 32) {
                        this.reader.forward();
                    }

                    String lb = this.scanLineBreak();

                    if (lb.length() == 0) {
                        if (!"\n".equals(lineBreak)) {
                            return lineBreak + breaks;
                        }

                        if (breaks.length() == 0) {
                            return " ";
                        }

                        return breaks.toString();
                    }

                    breaks.append(lb);
                    prefix = this.reader.prefix(3);
                } while (!"---".equals(prefix) && (!"...".equals(prefix) || !Constant.NULL_BL_T_LINEBR.has(this.reader.peek(3))));

                return "";
            } else {
                return "";
            }
        }
    }

    private String scanTagHandle(String name, Mark startMark) {
        char ch = this.reader.peek();

        if (ch != 33) {
            throw new ScannerException("while scanning a " + name, startMark, "expected \'!\', but found " + ch + "(" + ch + ")", this.reader.getMark());
        } else {
            int length = 1;

            ch = this.reader.peek(length);
            if (ch != 32) {
                while (Constant.ALPHA.has(ch)) {
                    ++length;
                    ch = this.reader.peek(length);
                }

                if (ch != 33) {
                    this.reader.forward(length);
                    throw new ScannerException("while scanning a " + name, startMark, "expected \'!\', but found " + ch + "(" + ch + ")", this.reader.getMark());
                }

                ++length;
            }

            String value = this.reader.prefixForward(length);

            return value;
        }
    }

    private String scanTagUri(String name, Mark startMark) {
        StringBuilder chunks = new StringBuilder();
        int length = 0;

        char ch;

        for (ch = this.reader.peek(length); Constant.URI_CHARS.has(ch); ch = this.reader.peek(length)) {
            if (ch == 37) {
                chunks.append(this.reader.prefixForward(length));
                length = 0;
                chunks.append(this.scanUriEscapes(name, startMark));
            } else {
                ++length;
            }
        }

        if (length != 0) {
            chunks.append(this.reader.prefixForward(length));
            boolean flag = false;
        }

        if (chunks.length() == 0) {
            throw new ScannerException("while scanning a " + name, startMark, "expected URI, but found " + ch + "(" + ch + ")", this.reader.getMark());
        } else {
            return chunks.toString();
        }
    }

    private String scanUriEscapes(String name, Mark startMark) {
        int length;

        for (length = 1; this.reader.peek(length * 3) == 37; ++length) {
            ;
        }

        Mark beginningMark = this.reader.getMark();

        ByteBuffer buff;

        for (buff = ByteBuffer.allocate(length); this.reader.peek() == 37; this.reader.forward(2)) {
            this.reader.forward();

            try {
                byte e = (byte) Integer.parseInt(this.reader.prefix(2), 16);

                buff.put(e);
            } catch (NumberFormatException numberformatexception) {
                throw new ScannerException("while scanning a " + name, startMark, "expected URI escape sequence of 2 hexadecimal numbers, but found " + this.reader.peek() + "(" + this.reader.peek() + ") and " + this.reader.peek(1) + "(" + this.reader.peek(1) + ")", this.reader.getMark());
            }
        }

        buff.flip();

        try {
            return UriEncoder.decode(buff);
        } catch (CharacterCodingException charactercodingexception) {
            throw new ScannerException("while scanning a " + name, startMark, "expected URI in UTF-8: " + charactercodingexception.getMessage(), beginningMark);
        }
    }

    private String scanLineBreak() {
        char ch = this.reader.peek();

        if (ch != 13 && ch != 10 && ch != 133) {
            if (ch != 8232 && ch != 8233) {
                return "";
            } else {
                this.reader.forward();
                return String.valueOf(ch);
            }
        } else {
            if (ch == 13 && 10 == this.reader.peek(1)) {
                this.reader.forward(2);
            } else {
                this.reader.forward();
            }

            return "\n";
        }
    }

    static {
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('0'), "\u0000");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('a'), "\u0007");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('b'), "\b");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('t'), "\t");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('n'), "\n");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('v'), "\u000b");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('f'), "\f");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('r'), "\r");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('e'), "\u001b");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf(' '), " ");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('\"'), "\"");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('\\'), "\\");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('N'), "\u0085");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('_'), " ");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('L'), "\u2028");
        ScannerImpl.ESCAPE_REPLACEMENTS.put(Character.valueOf('P'), "\u2029");
        ScannerImpl.ESCAPE_CODES.put(Character.valueOf('x'), Integer.valueOf(2));
        ScannerImpl.ESCAPE_CODES.put(Character.valueOf('u'), Integer.valueOf(4));
        ScannerImpl.ESCAPE_CODES.put(Character.valueOf('U'), Integer.valueOf(8));
    }

    private static class Chomping {

        private final Boolean value;
        private final int increment;

        public Chomping(Boolean value, int increment) {
            this.value = value;
            this.increment = increment;
        }

        public boolean chompTailIsNotFalse() {
            return this.value == null || this.value.booleanValue();
        }

        public boolean chompTailIsTrue() {
            return this.value != null && this.value.booleanValue();
        }

        public int getIncrement() {
            return this.increment;
        }
    }
}
