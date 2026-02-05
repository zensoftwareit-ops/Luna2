-- Creazione database Luna2
CREATE DATABASE IF NOT EXISTS luna2;
USE luna2;

-- Tabella utenti
CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100),
  nome VARCHAR(100),
  cognome VARCHAR(100),
  attivo BOOLEAN DEFAULT TRUE,
  data_creazione DATETIME DEFAULT CURRENT_TIMESTAMP,
  data_modifica DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Utente di test (username: admin, password: admin)
INSERT INTO users (username, password, email, nome, cognome, attivo) 
VALUES ('admin', 'admin', 'admin@luna2.com', 'Admin', 'System', TRUE)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- Ulteriori tabelle di base (per evitare errori di schema)
CREATE TABLE IF NOT EXISTS ordini (
  id INT PRIMARY KEY AUTO_INCREMENT,
  numero_ordine VARCHAR(50) NOT NULL UNIQUE,
  data_ordine DATETIME DEFAULT CURRENT_TIMESTAMP,
  cliente VARCHAR(255),
  importo DECIMAL(10,2),
  stato VARCHAR(50),
  user_id INT,
  data_creazione DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fatture (
  id INT PRIMARY KEY AUTO_INCREMENT,
  numero_fattura VARCHAR(50) NOT NULL UNIQUE,
  data_fattura DATETIME DEFAULT CURRENT_TIMESTAMP,
  cliente VARCHAR(255),
  importo DECIMAL(10,2),
  stato VARCHAR(50),
  user_id INT,
  data_creazione DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
