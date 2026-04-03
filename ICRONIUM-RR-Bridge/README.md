# ICRONIUM RR Bridge

Bridge web basato su Spring Boot per integrare Race Result con scheduler locali, persistenza JSON, WebSocket e upload verso sistemi esterni.

## Documentazione
- Manuale tecnico: [README.md](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/README.md)
- Manuale utente: [MANUALE-UTENTE.md](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/docs/MANUALE-UTENTE.md)

## Stack
- Java `21`
- Spring Boot `3.3.5`
- Gradle Wrapper
- HTML + Bootstrap + STOMP/WebSocket
- Persistenza file JSON su disco

## Requisiti
- JDK `21` installato
- Browser moderno
- Accesso a Race Result locale o remoto

## Avvio locale
Dalla root del progetto:

```bash
./gradlew bootRun
```

URL applicazione:
- [http://localhost:8087](http://localhost:8087)

## Import in Eclipse
1. `File -> Import -> Gradle -> Existing Gradle Project`
2. Seleziona la cartella del progetto
3. Completa l'import
4. Avvia con `bootRun` o come Spring Boot app

## Configurazione principale
File:
- [application.properties](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/resources/application.properties)

Impostazioni attuali:
- porta HTTP: `8087`
- timeout sessione: `12h`
- file di log: `logs/connector-rr.log`

## Pagine statiche
- `/index.html`
- `/dashboard.html`
- `/gara-dettaglio.html?eventId=<RRID>`
- `/timingpointView.html?eventId=<RRID>`
- `/rr-live.html?eventId=<RRID>`

## Autenticazione e sessione
Endpoint:
- `POST /api/auth/login`
- `POST /api/auth/login-local`
- `POST /api/auth/logout`
- `GET /api/auth/keepalive`
- `GET /api/auth/rr-open-url/{eventId}`

Note tecniche:
- la sessione Race Result viene mantenuta lato backend
- il frontend non gestisce direttamente il `pw` RR
- il keepalive periodico è necessario perché il solo WebSocket non rinnova la sessione HTTP
- login locale usa `pw=0`

## WebSocket
Handshake:
- `/ws`

Topic usati:
- `/topic/messages`
- `/topic/gare-updates`
- `/topic/rr-live-messages`
- `/topic/rr-live-updates`

Uso:
- log live UI
- refresh incrementale delle tabelle
- aggiornamento stato scheduler

## Persistenza
### Race Detail
Repository:
- [GaraDettaglioRepository.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/GaraDettaglioRepository.java)

Storage:
- `data/gara-dettaglio/<eventId>.json`

Persistenza tipica:
- righe source
- stato riga
- contatori read/sent/discarded
- blacklist
- dettagli ultimi cicli

### RR Live
Repository:
- [RrLiveRepository.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RrLiveRepository.java)

Storage:
- `data/rr-live/<eventId>.json`

## Logging
File di log:
- [connector-rr.log](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/logs/connector-rr.log)

Livelli configurati:
- `RRGaraDettaglioService=INFO`
- `PostUploader=INFO`

Nel log trovi:
- download file
- numero righe scaricate
- post batch
- errori download
- errori invio esterno
- attività scheduler

## Work directory esterna
Definita in:
- [IcroniumApplication.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/IcroniumApplication.java)

Percorso:
- `~/icronium-work`

## Componenti principali
### Controller REST
- [AuthController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/AuthController.java)
- [GareController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/GareController.java)
- [GaraDettaglioController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/GaraDettaglioController.java)
- [RrLiveController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/RrLiveController.java)

### Servizi principali
- [RaceResultClient.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RaceResultClient.java)
- [RRSessionService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RRSessionService.java)
- [RRGaraDettaglioService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RRGaraDettaglioService.java)
- [RrLiveService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RrLiveService.java)
- [PostUploader.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/PostUploader.java)
- [LiveUploader.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/LiveUploader.java)

## API principali
### Auth
- `POST /api/auth/login`
- `POST /api/auth/login-local`
- `POST /api/auth/logout`
- `GET /api/auth/keepalive`
- `GET /api/auth/rr-open-url/{eventId}`

### Gare
- `GET /api/gare`

### Gara dettaglio
- `GET /api/gare/{eventId}/dettaglio`
- `POST /api/gare/{eventId}/sync`
- `GET /api/gare/{eventId}/timing-points`
- `GET /api/gare/{eventId}/bib-chip`
- `GET /api/gare/{eventId}/rows`
- `GET /api/gare/{eventId}/rows/{rowId}/content`
- `POST /api/gare/{eventId}/file-passaggi`
- `DELETE /api/gare/{eventId}/rows/{rowId}`
- `DELETE /api/gare/{eventId}/rows`
- `POST /api/gare/{eventId}/rows/{rowId}/actions/{action}`
- `POST /api/gare/{eventId}/actions/{action}`
- `PUT /api/gare/{eventId}/rows/{rowId}/timing-point`
- `POST /api/gare/{eventId}/rows/{rowId}/duplicate`
- `POST /api/gare/{eventId}/rows/{rowId}/blacklist`
- `DELETE /api/gare/{eventId}/rows/{rowId}/blacklist`

### RR Live
- `POST /api/rr-live/sync`
- `GET /api/rr-live/state/{eventId}`
- `POST /api/rr-live/start/{eventId}`
- `POST /api/rr-live/stop/{eventId}`
- `POST /api/rr-live/reset/{eventId}`
- `DELETE /api/rr-live/rows/{eventId}/all`
- `POST /api/rr-live/{eventId}/rows/{rowKey}/actions/{action}`
- `DELETE /api/rr-live/{eventId}/rows/{rowKey}`

## Scheduler e flussi
### Race Detail scheduler
- parte su `PLAY`
- download immediato al primo giro
- cicli successivi regolati da `Download every`
- lettura incrementale del file
- righe con `*` ignorate in ingresso
- invio al `PostUploader` in batch da massimo `300`
- in caso di errore post, il contatore non avanza
- `REWIND` azzera lo stato di avanzamento

### RR Live scheduler
- sincronizza le API RR Live dell'evento
- esegue polling per riga
- `RESET` rimuove la memoria dell'ultimo file inviato

## Packaging e distribuzione
Cartelle presenti:
- `packaging/`
- `dist/`
- `release/`

Per una distribuzione pulita conviene:
1. svuotare `data/gara-dettaglio`
2. svuotare `data/rr-live`
3. opzionalmente pulire `logs`
4. generare il package dalla build corrente

## Manuale utente
Per l'uso operativo consulta:
- [MANUALE-UTENTE.md](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/docs/MANUALE-UTENTE.md)
