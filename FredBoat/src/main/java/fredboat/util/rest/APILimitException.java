package fredboat.util.rest;

public class APILimitException extends RuntimeException {

    public APILimitException() {
    }

    public APILimitException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public APILimitException(String string) {
        super(string);
    }
}
