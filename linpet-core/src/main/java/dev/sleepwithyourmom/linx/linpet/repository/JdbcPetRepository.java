package dev.sleepwithyourmom.linx.linpet.repository;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JDBC implementation of pet persistence.
 */
public class JdbcPetRepository implements PetRepository {
    private final DatabaseManager databaseManager;

    /**
     * Creates a JDBC pet repository.
     *
     * @param databaseManager database manager
     */
    public JdbcPetRepository(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("databaseManager must not be null");
        }
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(PetInstance pet) {
        String sql = databaseManager.sqlite()
            ? """
                INSERT INTO pets(instance_id, owner_id, template_id, level, experience, skill_points,
                    unlocked_skills, rarity, skin_id, custom_name, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(instance_id) DO UPDATE SET
                    owner_id=excluded.owner_id,
                    template_id=excluded.template_id,
                    level=excluded.level,
                    experience=excluded.experience,
                    skill_points=excluded.skill_points,
                    unlocked_skills=excluded.unlocked_skills,
                    rarity=excluded.rarity,
                    skin_id=excluded.skin_id,
                    custom_name=excluded.custom_name,
                    expires_at=excluded.expires_at
                """
            : """
                INSERT INTO pets(instance_id, owner_id, template_id, level, experience, skill_points,
                    unlocked_skills, rarity, skin_id, custom_name, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    owner_id=VALUES(owner_id),
                    template_id=VALUES(template_id),
                    level=VALUES(level),
                    experience=VALUES(experience),
                    skill_points=VALUES(skill_points),
                    unlocked_skills=VALUES(unlocked_skills),
                    rarity=VALUES(rarity),
                    skin_id=VALUES(skin_id),
                    custom_name=VALUES(custom_name),
                    expires_at=VALUES(expires_at)
                """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPet(statement, pet);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed saving pet " + pet.instanceId(), ex);
        }
    }

    @Override
    public Optional<PetInstance> find(UUID instanceId) {
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM pets WHERE instance_id = ?")) {
            statement.setString(1, instanceId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readPet(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading pet " + instanceId, ex);
        }
    }

    @Override
    public void delete(UUID instanceId) {
        try (Connection connection = databaseManager.connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement equipment = connection.prepareStatement("DELETE FROM equipment WHERE pet_instance_id = ?");
                 PreparedStatement pet = connection.prepareStatement("DELETE FROM pets WHERE instance_id = ?")) {
                equipment.setString(1, instanceId.toString());
                equipment.executeUpdate();
                pet.setString(1, instanceId.toString());
                pet.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed deleting pet " + instanceId, ex);
        }
    }

    @Override
    public List<PetInstance> findOwned(UUID ownerId) {
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM pets WHERE owner_id = ? ORDER BY template_id, level DESC")) {
            statement.setString(1, ownerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<PetInstance> pets = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    pets.add(readPet(resultSet));
                }
                return List.copyOf(pets);
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading owned pets for " + ownerId, ex);
        }
    }

    @Override
    public Map<Integer, PetInstance> findEquipped(UUID ownerId) {
        String sql = """
            SELECT e.slot, p.*
            FROM equipment e
            JOIN pets p ON p.instance_id = e.pet_instance_id
            WHERE e.player_id = ?
            ORDER BY e.slot
            """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, PetInstance> equipped = new LinkedHashMap<>();
                while (resultSet.next()) {
                    equipped.put(resultSet.getInt("slot"), readPet(resultSet));
                }
                return Map.copyOf(equipped);
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading equipment for " + ownerId, ex);
        }
    }

    @Override
    public void equip(UUID ownerId, int slot, UUID instanceId) {
        String sql = databaseManager.sqlite()
            ? """
                INSERT INTO equipment(player_id, slot, pet_instance_id)
                VALUES (?, ?, ?)
                ON CONFLICT(player_id, slot) DO UPDATE SET pet_instance_id=excluded.pet_instance_id
                """
            : """
                INSERT INTO equipment(player_id, slot, pet_instance_id)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE pet_instance_id=VALUES(pet_instance_id)
                """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
            statement.setInt(2, slot);
            statement.setString(3, instanceId.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed equipping pet " + instanceId + " in slot " + slot, ex);
        }
    }

    @Override
    public void unequip(UUID ownerId, int slot) {
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM equipment WHERE player_id = ? AND slot = ?")) {
            statement.setString(1, ownerId.toString());
            statement.setInt(2, slot);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed unequipping slot " + slot + " for " + ownerId, ex);
        }
    }

    @Override
    public void wipe(UUID ownerId) {
        try (Connection connection = databaseManager.connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement equipment = connection.prepareStatement("DELETE FROM equipment WHERE player_id = ?");
                 PreparedStatement pets = connection.prepareStatement("DELETE FROM pets WHERE owner_id = ?")) {
                equipment.setString(1, ownerId.toString());
                equipment.executeUpdate();
                pets.setString(1, ownerId.toString());
                pets.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed wiping player pets for " + ownerId, ex);
        }
    }

    private void bindPet(PreparedStatement statement, PetInstance pet) throws SQLException {
        statement.setString(1, pet.instanceId().toString());
        statement.setString(2, pet.ownerId().toString());
        statement.setString(3, pet.templateId());
        statement.setInt(4, pet.level());
        statement.setDouble(5, pet.experience());
        statement.setInt(6, pet.skillPoints());
        statement.setString(7, encodeSkills(pet.unlockedSkillIds()));
        statement.setString(8, pet.rarity().name());
        statement.setString(9, pet.skinId());
        statement.setString(10, pet.customName());
        if (pet.expiresAt() == null) {
            statement.setLong(11, -1L);
        } else {
            statement.setLong(11, pet.expiresAt().toEpochMilli());
        }
    }

    private PetInstance readPet(ResultSet resultSet) throws SQLException {
        long expiresAtMillis = resultSet.getLong("expires_at");
        Instant expiresAt = expiresAtMillis < 0L ? null : Instant.ofEpochMilli(expiresAtMillis);
        return new PetInstance(
            UUID.fromString(resultSet.getString("instance_id")),
            UUID.fromString(resultSet.getString("owner_id")),
            resultSet.getString("template_id"),
            resultSet.getInt("level"),
            resultSet.getDouble("experience"),
            resultSet.getInt("skill_points"),
            decodeSkills(resultSet.getString("unlocked_skills")),
            PetRarity.valueOf(resultSet.getString("rarity")),
            resultSet.getString("skin_id"),
            resultSet.getString("custom_name"),
            expiresAt
        );
    }

    private String encodeSkills(Set<String> skills) {
        return skills.stream()
            .sorted()
            .collect(Collectors.joining(","));
    }

    private Set<String> decodeSkills(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(encoded.split(","))
            .map(String::trim)
            .filter(skill -> !skill.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
