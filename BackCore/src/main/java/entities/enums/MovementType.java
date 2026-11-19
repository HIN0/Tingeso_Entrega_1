package entities.enums;

public enum MovementType {
    INCOME,        // new tool or stock income
    LOAN,          // loan
    RETURN,        // return
    REPAIR,        // unit sent to repair
    REPAIRED,      // repair finished: the unit goes back to stock
    DECOMMISSION,   // permanently removed
    MANUAL_DECREASE // manual stock decrease
}
