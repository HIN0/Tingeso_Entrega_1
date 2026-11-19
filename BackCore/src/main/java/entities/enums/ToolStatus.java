package entities.enums;

public enum ToolStatus {
    AVAILABLE,      // can be lent
    LOANED,         // no stock, units with clients
    REPAIRING,      // no stock, units under repair
    DECOMMISSIONED  // decommissioned (final)
}
