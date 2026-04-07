# ICRONIUM RR Bridge

Bridge web basato su Spring Boot per integrare Race Result e TZero con scheduler locali, persistenza JSON, WebSocket, viste operative dedicate e upload verso sistemi esterni.

## Documentazione
- Manuale tecnico: [README.md](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/README.md)
- Manuale utente: [MANUALE-UTENTE.md](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/docs/MANUALE-UTENTE.md)

## Stack
- Java `21`
- Spring Boot `3.3.5`
- Gradle Wrapper
- HTML + Bootstrap + STOMP/WebSocket
- Persistenza file JSON su disco
- `localStorage` browser per preferenze UI locali

## Requisiti
- JDK `21` installato
- Browser moderno
- Accesso a Race Result locale o remoto
- Accesso alle cartelle gara TZero, se usi la modalità `TZERO`

## Avvio locale
Dalla root del progetto:

```bash
./gradlew bootRun
```

URL applicazione:
- [http://localhost:8087](http://localhost:8087)

## Configurazione principale
File:
- [application.properties](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/resources/application.properties)

Impostazioni attuali:
- porta HTTP: `8087`
- timeout sessione: `12h`
- file di log: `logs/connector-rr.log`

## Modalità applicative
Sono supportate tre modalità:
- `RR`: login Race Result remoto
- `RR_LOCALE`: login Race Result locale
- `TZERO`: lavoro su cartelle gara TZero

## Pagine statiche
- `/index.html`
- `/dashboard.html`
- `/gara-dettaglio.html?eventId=<RRID>`
- `/timingpointView.html?eventId=<RRID>`
- `/fileView.html?eventId=<RRID>`
- `/readerView.html?eventId=<RRID>`
- `/rr-live.html?eventId=<RRID>`
- `/speaker-tp.html?token=<token>`

## Autenticazione e sessione
Endpoint:
- `POST /api/auth/login`
- `POST /api/auth/login-local`
- `POST /api/auth/login-tzero`
- `POST /api/auth/logout`
- `GET /api/auth/keepalive`
- `GET /api/auth/rr-open-url/{eventId}`

Note tecniche:
- la sessione RR viene mantenuta lato backend
- il browser conserva le credenziali RR online per il login rapido
- il keepalive periodico serve perché il solo WebSocket non rinnova la sessione HTTP
- login locale usa `pw=0`
- in `TZERO` la root folder viene persa dalla sessione solo se fai logout o reset lato browser/server

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
- refresh incrementale delle viste
- aggiornamento stato scheduler
- refresh `Reader View`

## Persistenza
### Race Detail
Repository:
- [GaraDettaglioRepository.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/GaraDettaglioRepository.java)

Storage:
- `data/gara-dettaglio/<eventId>.json`

Persistenza tipica:
- righe source
- stato riga
- contatori `READ / SENT / DISC / UNIQUE`
- blacklist
- dettagli ultimi cicli
- snapshot della modalità (`RR`, `RR_LOCALE`, `TZERO`)
- root TZero persistita per uso scheduler

### RR Live
Repository:
- [RrLiveRepository.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RrLiveRepository.java)

Storage:
- `data/rr-live/<eventId>.json`

### Reader View
Repository:
- [ReaderViewRepository.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/ReaderViewRepository.java)

Storage:
- `data/reader-view/<eventId>.json`

Persistenza tipica:
- elenco reader censiti
- ordine card
- ultimo stato scansione
- file odierni trovati per reader

### Speaker tokens
Storage:
- `data/speaker-tokens.json`

Uso:
- link pubblici tokenizzati per la pagina speaker

### TZero globale
Storage:
- `data/tzero/config.json`

Uso:
- root folder TZero persistita lato backend

### TZero per gara
Storage dentro la cartella gara:
- `config/timingpoints.json`
- `config/sources.json`

Uso:
- `timingpoints.json`: fonte autorevole dei timing point TZero
- `sources.json`: fonte autorevole delle sorgenti TZero configurate, incluse quelle remote WAN/LAN

### Browser
Persistenza locale in `localStorage` per:
- credenziali RR online
- start time, filtri e ordine nelle viste
- file visibili / timing point visibili
- stato collapse di alcune UI

## Logging
File di log:
- [connector-rr.log](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/logs/connector-rr.log)

Nel log trovi:
- download file
- numero righe scaricate
- post batch
- errori download
- errori invio esterno
- attività scheduler
- discovery reader

## Work directory esterna
Definita in:
- [IcroniumApplication.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/IcroniumApplication.java)

Percorso:
- `~/icronium-work`

Nota:
- in `TZERO` i file operativi restano nella cartella gara, soprattutto in `download/`
- `~/icronium-work` continua a essere utile per file locali, simulated file non TZero e operazioni di supporto

## Componenti principali
### Controller REST
- [AuthController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/AuthController.java)
- [GareController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/GareController.java)
- [GaraDettaglioController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/GaraDettaglioController.java)
- [RrLiveController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/RrLiveController.java)
- [TZeroController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/TZeroController.java)
- [ReaderViewController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/ReaderViewController.java)
- [PublicSpeakerController.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/controller/PublicSpeakerController.java)

### Servizi principali
- [RaceResultClient.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RaceResultClient.java)
- [RRSessionService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RRSessionService.java)
- [RRGaraDettaglioService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RRGaraDettaglioService.java)
- [RrLiveService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/RrLiveService.java)
- [ReaderViewService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/ReaderViewService.java)
- [TZeroConfigService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/TZeroConfigService.java)
- [SpeakerTokenService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/SpeakerTokenService.java)
- [PublicSpeakerService.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/PublicSpeakerService.java)
- [PostUploader.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/PostUploader.java)
- [LiveUploader.java](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/src/main/java/it/icron/icronium/connector/rr/integration/LiveUploader.java)

## API principali
### Auth
- `POST /api/auth/login`
- `POST /api/auth/login-local`
- `POST /api/auth/login-tzero`
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
- `POST /api/gare/{eventId}/simulated-file`
- `DELETE /api/gare/{eventId}/rows/{rowId}`
- `DELETE /api/gare/{eventId}/rows`
- `POST /api/gare/{eventId}/rows/{rowId}/actions/{action}`
- `POST /api/gare/{eventId}/actions/{action}`
- `PUT /api/gare/{eventId}/rows/{rowId}/timing-point`
- `POST /api/gare/{eventId}/rows/{rowId}/duplicate`
- `POST /api/gare/{eventId}/rows/{rowId}/blacklist`
- `DELETE /api/gare/{eventId}/rows/{rowId}/blacklist`

### TZero
- `GET /api/tzero/config`
- `POST /api/tzero/config`
- `GET /api/tzero/gare/{eventId}/timing-points`
- `POST /api/tzero/gare/{eventId}/timing-points`
- `DELETE /api/tzero/gare/{eventId}/timing-points/{name}`

### Reader View
- `GET /api/readers/{eventId}`
- `POST /api/readers/{eventId}`
- `PUT /api/readers/{eventId}/{readerId}`
- `DELETE /api/readers/{eventId}/{readerId}`
- `POST /api/readers/{eventId}/scan`
- `GET /api/readers/{eventId}/discover-wan`
- `GET /api/readers/{eventId}/{readerId}/files/{fileName}/content`
- `POST /api/readers/{eventId}/{readerId}/files/{fileName}/add-to-race`

### RR Live
- `POST /api/rr-live/sync`
- `GET /api/rr-live/state/{eventId}`
- `POST /api/rr-live/start/{eventId}`
- `POST /api/rr-live/stop/{eventId}`
- `POST /api/rr-live/reset/{eventId}`
- `DELETE /api/rr-live/rows/{eventId}/all`
- `POST /api/rr-live/{eventId}/rows/{rowKey}/actions/{action}`
- `DELETE /api/rr-live/{eventId}/rows/{rowKey}`

### Speaker pubblico
- `GET /api/gare/{eventId}/speaker-url/{timingPoint}`
- `GET /api/public/speaker/{token}`

## Scheduler e flussi
### Race Detail scheduler
- parte su `PLAY`
- download immediato al primo giro
- cicli successivi regolati da `Download every`
- lettura incrementale del file
- righe con `*` ignorate in ingresso
- supporto filtri `From time` / `To time` alla radice del parsing
- invio al `PostUploader` in batch da massimo `300`
- in caso di errore post, il contatore non avanza
- `REWIND` azzera lo stato di avanzamento
- in `TZERO` il post è simulato: serve a calcolare `discarded`, ma non invia a RR
- in `TZERO`, se la source è remota, il contenuto viene anche mirrorato in `download/`

### RR Live scheduler
- sincronizza le API RR Live dell'evento
- esegue polling per riga
- `RESET` rimuove la memoria dell'ultimo file inviato

### Reader scanner
- `Reader View` mantiene un proprio stato per gara
- `Scan LAN` prova gli IP della subnet `192.168.x.*` e censisce automaticamente i reader ICRON raggiungibili su `/files`
- `Scan WAN` legge i reader attivi dal servizio web e propone all'utente quali censire
- i file mostrati sono solo `.tags` e `.csv`

## Packaging e distribuzione
Cartelle presenti:
- `packaging/`
- `dist/`
- `release/`

Per una distribuzione pulita conviene:
1. svuotare i dati runtime non necessari
2. verificare `config/application.yml`
3. rigenerare `app.jar`
4. ricreare lo zip di release

## Manuale utente
Per l'uso operativo consulta:
- [MANUALE-UTENTE.md](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/docs/MANUALE-UTENTE.md)
