package services;

import models.BorrowRecord;
import utils.LibraryConstants;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/**
 * The single place where overdue days and fines are calculated, so the rule
 * (default {@link LibraryConstants#FINE_PER_DAY} per overdue day) lives in one reusable spot.
 */
public class FineService {

    private final int finePerDay;

    public FineService() {
        this(LibraryConstants.FINE_PER_DAY);
    }

    public FineService(int finePerDay) {
        if (finePerDay < 0) {
            throw new IllegalArgumentException("Fine per day cannot be negative.");
        }
        this.finePerDay = finePerDay;
    }

    public int getFinePerDay() {
        return finePerDay;
    }

    /** Number of days after the due date; 0 when returned on or before the due date. */
    public int calculateOverdueDays(LocalDate dueDate, LocalDate returnDate) {
        long days = ChronoUnit.DAYS.between(dueDate, returnDate);
        return days > 0 ? Math.toIntExact(days) : 0;
    }

    /** Fine for a book due on {@code dueDate} and returned on {@code returnDate}. */
    public int calculateFine(LocalDate dueDate, LocalDate returnDate) {
        return calculateOverdueDays(dueDate, returnDate) * finePerDay;
    }

    /**
     * Fine for a record as of a given day: the recorded fine for returned books,
     * or the fine accrued so far for books that are still out.
     */
    public int calculateFine(BorrowRecord record, LocalDate asOf) {
        if (record.isReturned()) {
            return record.getFineAmount();
        }
        return calculateFine(record.getDueDate(), asOf);
    }

    /** Overdue days for a record as of a given day (uses the return date once returned). */
    public int calculateOverdueDays(BorrowRecord record, LocalDate asOf) {
        LocalDate end = record.isReturned() ? record.getReturnDate() : asOf;
        return calculateOverdueDays(record.getDueDate(), end);
    }

    /** Sum of the fines already recorded on the given records. */
    public int getTotalFines(Collection<BorrowRecord> records) {
        int total = 0;
        for (BorrowRecord record : records) {
            total += record.getFineAmount();
        }
        return total;
    }

    /** Formats an amount such as 25 as "₹25". */
    public String formatAmount(int amount) {
        return LibraryConstants.CURRENCY_SYMBOL + amount;
    }
}
