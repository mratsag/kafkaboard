ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS avatar_color VARCHAR(7) DEFAULT '#6366f1';

UPDATE users
SET avatar_color = '#6366f1'
WHERE avatar_color IS NULL;
