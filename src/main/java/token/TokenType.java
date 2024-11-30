package token;

public enum TokenType {
    PROGRAM("Program"),
    CLASS("class"),
    EXTENDS("extends"),
    IS("is"),
    END("end"),
    VAR("var"),
    METHOD("method"),
    THIS("this"),
    WHILE("while"),
    LOOP("loop"),
    IF("if"),
    THEN("then"),
    ELSE("else"),
    RETURN("return"),

    LEFT_BRACE("\\{"),
    RIGHT_BRACE("\\}"),
    LEFT_PAREN("\\("),
    RIGHT_PAREN("\\)"),
    LEFT_BRACKET("\\["),
    RIGHT_BRACKET("\\]"),
    ASSIGN(":="),
    COLON(":"),
    DOT("\\."),
    COMMA(","),

    ARRAY("Array"),
    LIST("List"),
    ANYREF("AnyRef"),
    ANYVALUE("AnyValue"),
    INTEGER("Integer"),
    REAL("Real"),
    BOOLEAN("Boolean"),

    BOOLEAN_LITERAL("true|false"),
    IDENTIFIER("[a-zA-Z_][a-zA-Z0-9_]*"),
    REAL_LITERAL("\\d+\\.\\d+"),
    INTEGER_LITERAL("\\d+"),
    STRING_LITERAL("\"([^\"]*)\""),

    WHITESPACE("[ \t\f\r\n]+");

    public final String pattern;
    public Long position = 0L;

    TokenType(String pattern) {
        this.pattern = pattern;
    }

    TokenType(String pattern, Long position) {
        this.pattern = pattern;
        this.position = position;
    }
}
