package exceptions;

/** Wraps low-level file read/write failures with a readable message. */
public class StorageException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
