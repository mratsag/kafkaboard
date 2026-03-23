ALTER TABLE clusters
  ADD COLUMN IF NOT EXISTS security_protocol
    VARCHAR(20) DEFAULT 'PLAINTEXT',
  ADD COLUMN IF NOT EXISTS sasl_mechanism
    VARCHAR(20),
  ADD COLUMN IF NOT EXISTS sasl_username
    VARCHAR(255),
  ADD COLUMN IF NOT EXISTS sasl_password_encrypted
    TEXT;
