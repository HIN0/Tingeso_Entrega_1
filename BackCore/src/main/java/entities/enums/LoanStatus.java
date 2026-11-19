package entities.enums;

public enum LoanStatus {
    ACTIVE,     // open, within the due date
    LATE,       // open, past the due date
    RECEIVED,   // returned, payment pending
    CLOSED      // returned and paid
}
