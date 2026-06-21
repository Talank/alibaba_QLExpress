package com.alibaba.qlexpress4.aparser;

import com.alibaba.qlexpress4.exception.QLErrorCodes;
import com.alibaba.qlexpress4.exception.QLException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

public class QLExtendLexer extends QLexer {
    
    private final String script;
    
    private final InterpolationMode interpolationMode;
    
    private final String selectorStart;
    
    private final String selectorEnd;
    
    private final boolean strictNewLines;
    
    public QLExtendLexer(CharStream input, String script, InterpolationMode interpolationMode, String selectorStart,
        String selectorEnd, boolean strictNewLines) {
        super(normalizeCRLF(input));
        this.script = script.indexOf('\r') < 0 ? script : script.replace("\r\n", "\n").replace("\r", "\n");
        this.interpolationMode = interpolationMode;
        this.selectorStart = selectorStart;
        this.selectorEnd = selectorEnd;
        this.strictNewLines = strictNewLines;
    }
    
    @Override
    public Token nextToken() {
        Token token = super.nextToken();
        // In non-strict mode, skip NEWLINE tokens
        if (!strictNewLines && token.getType() == QLexer.NEWLINE) {
            // Skip NEWLINEs by recursively calling nextToken()
            return nextToken();
        }
        return token;
    }
    
    @Override
    protected InterpolationMode getInterpolationMode() {
        return interpolationMode;
    }
    
    @Override
    protected String getSelectorStart() {
        return selectorStart;
    }
    
    @Override
    protected void consumeSelectorVariable() {
        StringBuilder t = new StringBuilder();
        int selectorEndLength = selectorEnd.length();
        char lastCharOfSelector = selectorEnd.charAt(selectorEndLength - 1);
        
        t.ensureCapacity(selectorEndLength * 2);
        
        while (true) {
            int curChInt = _input.LA(1);
            if (curChInt == Token.EOF || curChInt == '\n') {
                // mismatch
                throwScannerException(t.toString(), "unterminated selector");
            }
            char curCh = (char)curChInt;
            t.append(curCh);
            _input.consume();
            
            if (curCh == lastCharOfSelector && t.length() >= selectorEndLength) {
                if (checkEndsWith(t, selectorEnd)) {
                    // match
                    String text = t.toString();
                    setText(text.substring(0, text.length() - selectorEndLength));
                    popMode();
                    break;
                }
            }
        }
    }
    
    private static CharStream normalizeCRLF(CharStream input) {
        String text = input.getText(Interval.of(0, input.size() - 1));
        if (text.indexOf('\r') < 0) {
            return input;
        }
        return CharStreams.fromString(text.replace("\r\n", "\n").replace("\r", "\n"));
    }

    @Override
    protected void throwScannerException(String lexeme, String reason) {
        throw QLException.reportScannerErr(script,
            this._tokenStartCharIndex,
            this._tokenStartLine,
            this._tokenStartCharPositionInLine,
            lexeme,
            QLErrorCodes.SYNTAX_ERROR.name(),
            reason);
    }
    
    private boolean checkEndsWith(StringBuilder sb, String suffix) {
        int suffixLength = suffix.length();
        int sbLength = sb.length();
        
        for (int i = 0; i < suffixLength; i++) {
            if (sb.charAt(sbLength - suffixLength + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
