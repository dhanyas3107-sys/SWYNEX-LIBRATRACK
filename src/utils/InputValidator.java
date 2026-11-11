package utils;

import exceptions.InvalidInputException;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Stateless validation and parsing helpers shared by the models, services and
 * console UI. Every method either returns a cleaned value or throws an
 * {@link InvalidInputException} carrying a message that is safe to show the user.
 */
public final class InputValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^\\+?[0-9]{10,13}$");
    private static final Pattern ISBN_PATTERN = Pattern.compile("^([0-9]{13}|[0-9]{9}[0-9X])$");

    private InputValidator() {
        // utility class - never instantiated
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Trims the text and rejects blank, over-long or control-character input. */
    public static String requireText(String value, String fieldName) throws InvalidInputException {
        if (isBlank(value)) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > LibraryConstants.MAX_TEXT_LENGTH) {
            throw new InvalidInputException(fieldName + " cannot be longer than "
                    + LibraryConstants.MAX_TEXT_LENGTH + " characters.");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                throw new InvalidInputException(fieldName + " contains invalid control characters.");
            }
        }
        return trimmed;
    }

    public static int validateId(int id, String fieldName) throws InvalidInputException {
        if (id <= 0) {
            throw new InvalidInputException(fieldName + " must be a positive whole number.");
        }
        return id;
    }

    public static String validateEmail(String email) throws InvalidInputException {
        String trimmed = requireText(email, "Email address");
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("'" + trimmed + "' is not a valid email address (example: name@example.com).");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /** Accepts 10-13 digits with an optional leading '+'; spaces and hyphens are ignored. */
    public static String validateContactNumber(String contact) throws InvalidInputException {
        String cleaned = requireText(contact, "Contact number").replaceAll("[\\s-]", "");
        if (!CONTACT_PATTERN.matcher(cleaned).matches()) {
            throw new InvalidInputException("Contact number must contain 10 to 13 digits (optional leading +).");
        }
        return cleaned;
    }

    /** Accepts ISBN-10 or ISBN-13 (hyphens/spaces ignored) and stores it in a normalised form. */
    public static String validateIsbn(String isbn) throws InvalidInputException {
        String cleaned = requireText(isbn, "ISBN").replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!ISBN_PATTERN.matcher(cleaned).matches()) {
            throw new InvalidInputException("ISBN must contain 10 or 13 digits (an ISBN-10 may end with X).");
        }
        return cleaned;
    }

    public static int validatePublicationYear(int year) throws InvalidInputException {
        int currentYear = Year.now().getValue();
        if (year < LibraryConstants.MIN_PUBLICATION_YEAR || year > currentYear) {
            throw new InvalidInputException("Publication year must be between "
                    + LibraryConstants.MIN_PUBLICATION_YEAR + " and " + currentYear + ".");
        }
        return year;
    }

    public static int validateYearOfStudy(int year) throws InvalidInputException {
        if (year < LibraryConstants.MIN_YEAR_OF_STUDY || year > LibraryConstants.MAX_YEAR_OF_STUDY) {
            throw new InvalidInputException("Year of study must be between "
                    + LibraryConstants.MIN_YEAR_OF_STUDY + " and " + LibraryConstants.MAX_YEAR_OF_STUDY + ".");
        }
        return year;
    }

    public static int parseInt(String raw, String fieldName) throws InvalidInputException {
        if (isBlank(raw)) {
            throw new InvalidInputException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a whole number (you entered '" + raw.trim() + "').");
        }
    }

    /** Parses an ISO date (yyyy-MM-dd). */
    public static LocalDate parseDate(String raw, String fieldName) throws InvalidInputException {
        if (isBlank(raw)) {
            throw new InvalidInputException(fieldName + " is required.");
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(fieldName + " must be a valid date in yyyy-MM-dd format (e.g. 2026-11-30).");
        }
    }
}
