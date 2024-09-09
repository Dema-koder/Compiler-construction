package lexical;

public enum TokenType {
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

    IDENTIFIER("[a-zA-Z_][a-zA-Z0-9_]*"),
    INTEGER_LITERAL("\\d+"),
    REAL_LITERAL("\\d+\\.\\d+"),
    STRING_LITERAL("\"([^\"]*)\""),
    BOOLEAN_LITERAL("true|false"),

    WHITESPACE("[ \t\f\r\n]+");

    public final String pattern;

    TokenType(String pattern) {
        this.pattern = pattern;
    }
}
