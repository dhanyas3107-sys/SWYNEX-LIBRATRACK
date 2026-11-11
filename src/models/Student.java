package models;

import exceptions.InvalidInputException;
import utils.InputValidator;
import utils.LibraryConstants;

/** A library member who can borrow books. Adds department and year of study to {@link User}. */
public class Student extends User {

    private String department;
    private int yearOfStudy;

    public Student(int userId, String name, String email, String contactNumber,
                   String department, int yearOfStudy) throws InvalidInputException {
        super(userId, name, email, contactNumber);   // common fields validated by User
        this.department = InputValidator.requireText(department, "Department");
        this.yearOfStudy = InputValidator.validateYearOfStudy(yearOfStudy);
    }

    public String getDepartment() {
        return department;
    }

    public int getYearOfStudy() {
        return yearOfStudy;
    }

    public void setDepartment(String department) throws InvalidInputException {
        this.department = InputValidator.requireText(department, "Department");
    }

    public void setYearOfStudy(int yearOfStudy) throws InvalidInputException {
        this.yearOfStudy = InputValidator.validateYearOfStudy(yearOfStudy);
    }

    @Override
    public String displayRole() {
        return "Student of " + department + " (Year " + yearOfStudy + ") - may borrow up to "
                + getBorrowLimit() + " books";
    }

    @Override
    public String getUserType() {
        return "STUDENT";
    }

    @Override
    public int getBorrowLimit() {
        return LibraryConstants.MAX_ACTIVE_BORROWS;
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | Department: " + department + " | Year: " + yearOfStudy;
    }
}
