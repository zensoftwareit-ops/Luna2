# Changelog

All notable changes to Luna2 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-01-15

### Added
- **Dashboard Completa**: KPI aziendali, grafici vendite, alert scadenze
- **CRM / Lead Management**: Gestione completa opportunità commerciali
  - Stati lead: Nuovo, Contattato, Qualificato, Proposta, Negoziazione, Vinto, Perso
  - Sistema di tagging e categorizzazione
  - Conversione lead in cliente
- **Anagrafica Clienti**: Gestione completa clienti e contatti
  - Dati fiscali (P.IVA, C.F., SDI)
  - Contatti multipli per cliente
  - Storico documenti e fatturato
- **Anagrafica Fornitori**: Gestione fornitori con dati fiscali
- **Catalogo Prodotti**: 
  - Gestione prodotti con categorie
  - Listini prezzi multipli
  - Codici prodotto e barcode
- **Documenti Commerciali**:
  - Preventivi con numerazione automatica
  - Ordini cliente e fornitore
  - DDT (Documenti di Trasporto)
  - Fatture con calcolo IVA
  - Generazione PDF per tutti i documenti
- **Magazzino**:
  - Gestione giacenze in tempo reale
  - Movimenti di carico/scarico
  - Alert sottoscorta
- **Report e Analytics**:
  - Report vendite per periodo
  - Analisi prodotti più venduti
  - Report clienti
  - Export Excel/CSV
- **REST API**: Endpoint JSON per integrazione sistemi esterni
- **Sicurezza**:
  - Password hashing con BCrypt
  - Protezione CSRF
  - Session management sicuro
  - Validazione input lato server
- **UI/UX**:
  - Interfaccia moderna con Bootstrap 5
  - Responsive design per mobile/tablet
  - DataTables per liste con ricerca e paginazione
  - Sidebar di navigazione intuitiva

### Technical Stack
- Java 11
- Struts2 6.3.0
- Hibernate 5.6.15 + C3P0
- MySQL 8.0
- Bootstrap 5.3.0
- iText 5.5.13.3 (PDF generation)
- Apache POI 5.2.3 (Excel export)
- Log4j2 (logging)

### Infrastructure
- Maven build system
- Jetty embedded server for development
- Tomcat deployment for production
- Setup script per installazione automatica

## [1.0.0] - 2025-06-01

### Added
- Initial release
- Basic CRUD for clients
- Simple product catalog
- Basic invoice generation

---

## Roadmap

### [2.1.0] - Q2 2026 (Planned)
- [ ] Fatturazione Elettronica XML (FatturaPA)
- [ ] Integrazione PEC per invio documenti
- [ ] Dashboard personalizzabili
- [ ] Mobile app (iOS/Android)
- [ ] API REST v2 con autenticazione OAuth2
- [ ] Backup automatico cloud
- [ ] Multi-azienda support
- [ ] Multi-lingua (EN, ES, FR, DE)

### [2.2.0] - Q3 2026 (Planned)
- [ ] Gestione progetti e timesheet
- [ ] Gestione dipendenti e presenze
- [ ] Contratti e scadenze contrattuali
- [ ] Integrazione sistemi pagamento (Stripe, PayPal)
- [ ] Firma digitale documenti
- [ ] Workflow approvazioni
- [ ] Notifiche push e email automatiche
- [ ] Chat interna per collaborazione team

### [3.0.0] - Q4 2026 (Planned)
- [ ] AI per previsioni vendite
- [ ] Analisi predittiva giacenze
- [ ] Suggerimenti automatici prezzi
- [ ] OCR per scansione fatture fornitori
- [ ] Business Intelligence avanzata
- [ ] API GraphQL
- [ ] Microservices architecture
- [ ] Kubernetes deployment

## Versioning

We use [SemVer](http://semver.org/) for versioning. 

- **MAJOR** version: incompatible API changes
- **MINOR** version: new functionality in backwards compatible manner  
- **PATCH** version: backwards compatible bug fixes

## Support

For questions and support:
- Email: support@zensoftware.it
- Documentation: https://docs.gestionaleluna.it
- Issue Tracker: GitHub Issues

---

Copyright © 2026 Zen Software. All rights reserved.
