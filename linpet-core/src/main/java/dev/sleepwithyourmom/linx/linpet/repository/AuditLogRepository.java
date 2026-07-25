package dev.sleepwithyourmom.linx.linpet.repository;

import java.util.UUID;

/**
 * Append-only audit log repository for administrative and economic actions.
 */
public interface AuditLogRepository {
    /**
     * Records an audit entry.
     *
     * @param actor actor UUID, or {@code null} for console/system
     * @param action stable action name
     * @param target target identifier
     * @param detail human-readable detail
     */
    void record(UUID actor, String action, String target, String detail);
}
