package token;

public record Token(TokenType type, String value, Long position) {

    @Override
    public String toString() {
        return String.format("Token[type=%s, value='%s', position=%d]", type.name(), value, position);
    }
}
