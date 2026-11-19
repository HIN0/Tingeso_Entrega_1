package entities.enums;

public enum MovementType {
    INCOME,        // Registro de herramienta nueva
    LOAN,          // Préstamo
    RETURN,        // Devolución
    REPAIR,        // En reparación
    REPAIRED,      // Reparación terminada: la unidad vuelve al stock
    DECOMMISSION,   // Baja definitiva
    MANUAL_DECREASE // Ajuste manual para reducir stock
}
