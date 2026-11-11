package exceptions;

/** Thrown when a student ID is not registered. */
public class StudentNotFoundException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public StudentNotFoundException(String message) {
        super(message);
    }

    public StudentNotFoundException(int studentId) {
        super("No student is registered with ID " + studentId + ".");
    }
}
