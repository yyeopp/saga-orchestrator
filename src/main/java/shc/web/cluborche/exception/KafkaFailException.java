package shc.web.cluborche.exception;

public class KafkaFailException extends Exception {
    public KafkaFailException() {
    }

    public KafkaFailException(String message) {
        super(message);
    }

    public KafkaFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaFailException(Throwable cause) {
        super(cause);
    }

    public KafkaFailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
