package models;

import exceptions.InvalidInputException;
import utils.InputValidator;

/** Library staff who manage books, members and transactions. Staff do not borrow books. */
public class Librarian extends User {

    private final String employeeId;
    private String designation;

    public Librarian(int userId, String name, String email, String contactNumber,
                     String employeeId, String designation) throws InvalidInputException {
        super(userId, name, email, contactNumber);
        this.employeeId = InputValidator.requireText(employeeId, "Employee ID");
        this.designation = InputValidator.requireText(designation, "Designation");
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) throws InvalidInputException {
        this.designation = InputValidator.requireText(designation, "Designation");
    }

    @Override
    public String displayRole() {
        return designation + " - manages books, members, issues, returns and fines";
    }

    @Override
    public String getUserType() {
        return "LIBRARIAN";
    }

    @Override
    public int getBorrowLimit() {
        return 0; // staff administer the library; they do not borrow from it
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | Employee ID: " + employeeId + " | Designation: " + designation;
    }
}
