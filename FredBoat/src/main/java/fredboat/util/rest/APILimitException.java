package fredboat.util.rest;

import fredboat.commandmeta.MessagingException;

public class APILimitException extends MessagingException {

    public APILimitException(String string) {
        super(string);
    }
}
