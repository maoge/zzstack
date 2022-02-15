package dbtest.service.exception;

@SuppressWarnings("serial")
public class InitializeException extends RuntimeException {

    public InitializeException(String message) {
        super(message);
    }

    public InitializeException(Exception ex) {
        super(ex);
    }

    public InitializeException(String message, Exception ex) {
        super(message, ex);
    }

}
