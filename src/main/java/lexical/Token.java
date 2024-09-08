package lexical;

public record Token(TokenType type, String value) {

    @Override
    public String toString() {
        return String.format("Token[type=%s, value='%s']", type.name(), value);
    }
}
