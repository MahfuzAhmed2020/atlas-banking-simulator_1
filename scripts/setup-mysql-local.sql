-- Run once in MySQL Workbench using a local MySQL administrator connection.
-- For fictional banking data only. Change the example password on a shared machine.
CREATE DATABASE IF NOT EXISTS atlas_bank
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'atlas'@'localhost' IDENTIFIED BY 'atlas_dev_password';
CREATE USER IF NOT EXISTS 'atlas'@'127.0.0.1' IDENTIFIED BY 'atlas_dev_password';
GRANT ALL PRIVILEGES ON atlas_bank.* TO 'atlas'@'localhost';
GRANT ALL PRIVILEGES ON atlas_bank.* TO 'atlas'@'127.0.0.1';
FLUSH PRIVILEGES;

-- On first backend startup, Flyway automatically creates the tables in atlas_bank.
-- Do not run backend migrations manually through Workbench.
