package services;

/** Immutable snapshot of the library's headline numbers. */
public record LibraryStatistics(
        int totalBooks,
        int availableBooks,
        int borrowedBooks,
        int totalStudents,
        int totalTransactions,
        int activeTransactions,
        int totalFines) {
}
