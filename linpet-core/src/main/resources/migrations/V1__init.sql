CREATE TABLE IF NOT EXISTS pets (
    instance_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(128) NOT NULL,
    level INTEGER NOT NULL,
    experience DOUBLE NOT NULL,
    skill_points INTEGER NOT NULL,
    unlocked_skills TEXT NOT NULL,
    rarity VARCHAR(32) NOT NULL,
    skin_id VARCHAR(512) NOT NULL,
    custom_name VARCHAR(128),
    expires_at BIGINT NOT NULL DEFAULT -1
);

CREATE TABLE IF NOT EXISTS equipment (
    player_id VARCHAR(36) NOT NULL,
    slot INTEGER NOT NULL,
    pet_instance_id VARCHAR(36) NOT NULL UNIQUE,
    PRIMARY KEY(player_id, slot),
    FOREIGN KEY(pet_instance_id) REFERENCES pets(instance_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
    auction_id VARCHAR(36) PRIMARY KEY,
    seller_id VARCHAR(36) NOT NULL,
    pet_instance_id VARCHAR(36) NOT NULL,
    pet_template_id VARCHAR(128) NOT NULL,
    current_bid VARCHAR(64) NOT NULL,
    highest_bidder VARCHAR(36) NOT NULL,
    ends_at BIGINT NOT NULL,
    version BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    actor_id VARCHAR(36) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target VARCHAR(128) NOT NULL,
    detail TEXT NOT NULL,
    created_at VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS shop_purchases (
    player_id VARCHAR(36) NOT NULL,
    pet_template_id VARCHAR(128) NOT NULL,
    purchase_day VARCHAR(16) NOT NULL,
    amount INTEGER NOT NULL,
    PRIMARY KEY(player_id, pet_template_id, purchase_day)
);
