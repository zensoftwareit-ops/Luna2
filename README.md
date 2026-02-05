# Luna2 - Gestionale Cloud per PMI

Luna2 è un software gestionale completo sviluppato in Java per la gestione aziendale di piccole e medie imprese. Include moduli per CRM, gestione clienti/fornitori, prodotti, magazzino, documenti commerciali (preventivi, ordini, DDT, fatture) e reporting.

## Tecnologie Utilizzate

- **Backend**: Java 11, Maven
- **Framework Web**: Struts2 6.3.0
- **ORM**: Hibernate 5.6.15 + C3P0 Connection Pool
- **Database**: MySQL 8.0
- **Frontend**: JSP, HTML5, CSS3, Bootstrap 5.3.0
- **Librerie Aggiuntive**:
  - iText 5.5.13.3 (generazione PDF)
  - Apache POI 5.2.3 (export Excel)
  - BCrypt (hashing password)
  - Log4j2 (logging)
  - Jackson (JSON)

## Funzionalità Principali

### Moduli Implementati

1. **Dashboard**
   - Panoramica KPI aziendali
   - Fatturato mensile
   - Preventivi aperti
   - Clienti attivi
   - Scadenze

2. **CRM / Lead Management**
   - Gestione lead e opportunità
   - Stati: Nuovo, Contattato, Qualificato, Proposta, Negoziazione, Vinto, Perso
   - Tag e categorizzazione
   - Conversione lead in cliente

3. **Anagrafica Clienti**
   - Gestione completa anagrafica clienti
   - Dati fiscali (P.IVA, C.F., Codice Destinatario SDI)
   - Contatti multipli per cliente
   - Storico documenti e fatturato

4. **Anagrafica Fornitori**
   - Gestione fornitori
   - Dati di contatto e fiscali
   - Condizioni di pagamento

5. **Prodotti e Listini**
   - Catalogo prodotti
   - Categorie e sottocategorie
   - Listini prezzi multipli
   - Codici prodotto e barcode

6. **Documenti Commerciali**
   - Preventivi
   - Ordini cliente/fornitore
   - DDT (Documenti di Trasporto)
   - Fatture (attive/passive)
   - Numerazione automatica progressiva
   - Calcolo IVA e totali

7. **Magazzino**
   - Gestione giacenze
   - Movimenti di carico/scarico
   - Inventario
   - Storico movimenti

8. **Report e Statistiche**
   - Report vendite per periodo
   - Analisi prodotti più venduti
   - Report clienti
   - Export Excel/CSV
   - Grafici e dashboard

## Requisiti di Sistema

- Java JDK 11 o superiore
- Apache Maven 3.6+
- MySQL Server 8.0+
- Apache Tomcat 9.0+ o Jetty 9.4+ (per deployment)
- 2 GB RAM minimo
- 500 MB spazio disco

## Installazione

### 1. Clonare il Repository

```bash
git clone <repository-url>
cd Luna2
```

### 2. Configurare il Database MySQL

Creare il database e l'utente:

```sql
CREATE DATABASE luna2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'luna2_user'@'localhost' IDENTIFIED BY 'luna2_password';
GRANT ALL PRIVILEGES ON luna2.* TO 'luna2_user'@'localhost';
FLUSH PRIVILEGES;
```

Eseguire lo script di creazione schema:

```bash
mysql -u luna2_user -p luna2 < src/main/resources/database/schema.sql
```

### 3. Configurare Hibernate

Copiare il template di configurazione:

```bash
cp src/main/resources/hibernate.cfg.xml.template src/main/resources/hibernate.cfg.xml
```

Modificare `hibernate.cfg.xml` con i dati del database:

```xml
<property name="hibernate.connection.url">jdbc:mysql://localhost:3306/luna2?useSSL=false&amp;serverTimezone=Europe/Rome</property>
<property name="hibernate.connection.username">luna2_user</property>
<property name="hibernate.connection.password">luna2_password</property>
```

### 4. Compilare il Progetto

```bash
mvn clean install
```

### 5. Avviare l'Applicazione

#### Opzione A: Usando Jetty (Development)

```bash
mvn jetty:run
```

L'applicazione sarà disponibile su: `http://localhost:8080/luna2`

#### Opzione B: Deploy su Tomcat (Production)

```bash
# Generare il WAR
mvn clean package

# Copiare il WAR in Tomcat
cp target/luna2.war /path/to/tomcat/webapps/

# Avviare Tomcat
/path/to/tomcat/bin/catalina.sh start
```

L'applicazione sarà disponibile su: `http://localhost:8080/luna2`

## Credenziali di Accesso Predefinite

Dopo l'installazione del database, utilizzare le seguenti credenziali:

- **Username**: `admin`
- **Password**: `admin123`

**IMPORTANTE**: Cambiare la password amministratore dopo il primo accesso!

## Struttura del Progetto

```
Luna2/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── it/zensoftware/luna2/
│   │   │       ├── action/          # Struts2 Actions (Controllers)
│   │   │       ├── dao/              # Data Access Objects
│   │   │       ├── model/            # Entities (JPA/Hibernate)
│   │   │       ├── filter/           # Servlet Filters
│   │   │       ├── interceptor/      # Struts2 Interceptors
│   │   │       └── util/             # Utility Classes
│   │   ├── resources/
│   │   │   ├── database/             # SQL Scripts
│   │   │   ├── hibernate.cfg.xml    # Hibernate Configuration
│   │   │   └── log4j2.xml           # Logging Configuration
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   ├── jsp/              # JSP Views
│   │       │   ├── web.xml           # Web Descriptor
│   │       │   └── struts.xml        # Struts Configuration
│   │       └── assets/               # Static Resources (CSS, JS, images)
│   └── test/                         # Unit Tests
├── pom.xml                           # Maven Configuration
└── README.md                         # This file
```

## Configurazione

### Configurazione Database

Modificare `src/main/resources/hibernate.cfg.xml`:

```xml
<!-- Database connection settings -->
<property name="hibernate.connection.driver_class">com.mysql.cj.jdbc.Driver</property>
<property name="hibernate.connection.url">jdbc:mysql://localhost:3306/luna2</property>
<property name="hibernate.connection.username">luna2_user</property>
<property name="hibernate.connection.password">luna2_password</property>

<!-- Connection pool settings -->
<property name="hibernate.c3p0.min_size">5</property>
<property name="hibernate.c3p0.max_size">20</property>
<property name="hibernate.c3p0.timeout">300</property>
<property name="hibernate.c3p0.max_statements">50</property>
```

### Configurazione Logging

Modificare `src/main/resources/log4j2.xml` per personalizzare i livelli di log.

## Utilizzo

### Accesso all'Applicazione

1. Aprire il browser su `http://localhost:8080/luna2`
2. Inserire username e password
3. Accedere alla dashboard principale

### Gestione Clienti

1. Navigare su **Clienti** nel menu laterale
2. Click su **Nuovo Cliente** per aggiungere un cliente
3. Compilare i dati anagrafici e fiscali
4. Salvare

### Creazione Preventivo

1. Navigare su **Documenti → Preventivi**
2. Click su **Nuovo Preventivo**
3. Selezionare il cliente
4. Aggiungere le righe prodotto
5. Il sistema calcolerà automaticamente totali e IVA
6. Salvare e generare PDF

### Gestione Magazzino

1. Navigare su **Magazzino → Giacenze**
2. Per effettuare un carico: **Nuovo Carico**
3. Selezionare prodotto e quantità
4. Il sistema aggiornerà automaticamente le giacenze

### Report e Statistiche

1. Navigare su **Report**
2. Selezionare tipo di report (Vendite, Prodotti, Clienti)
3. Impostare periodo di riferimento
4. Visualizzare i dati o esportare in Excel

## Backup e Manutenzione

### Backup Database

```bash
# Backup completo
mysqldump -u luna2_user -p luna2 > backup_luna2_$(date +%Y%m%d).sql

# Backup solo dati
mysqldump -u luna2_user -p --no-create-info luna2 > backup_data_$(date +%Y%m%d).sql
```

### Ripristino Database

```bash
mysql -u luna2_user -p luna2 < backup_luna2_20240101.sql
```

## Troubleshooting

### Errore di Connessione Database

Verificare che:
- MySQL sia in esecuzione: `systemctl status mysql`
- Le credenziali in `hibernate.cfg.xml` siano corrette
- Il database esista: `SHOW DATABASES;`
- L'utente abbia i permessi: `SHOW GRANTS FOR 'luna2_user'@'localhost';`

### OutOfMemoryError

Aumentare la memoria JVM:

```bash
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"
mvn jetty:run
```

## Sicurezza

- Le password sono hashate con BCrypt
- Protezione CSRF tramite Struts2 Token
- Session timeout configurabile
- Autenticazione richiesta per tutte le pagine (tranne login)
- Validazione input lato server
- Prepared statements per prevenire SQL Injection

## Performance

- Connection pooling C3P0 per ottimizzare connessioni DB
- Caching di secondo livello Hibernate (opzionale)
- Lazy loading per ridurre query
- Indici database ottimizzati

## Licenza

Copyright © 2026 Zen Software. Tutti i diritti riservati.

## Supporto

Per supporto tecnico o segnalazione bug:

- Email: support@zensoftware.it
- Documentazione: https://docs.gestionaleluna.it

## Crediti

Sviluppato da Zen Software  
Website: https://www.gestionaleluna.it

---

**Versione**: 2.0.0  
**Data Rilascio**: Gennaio 2026