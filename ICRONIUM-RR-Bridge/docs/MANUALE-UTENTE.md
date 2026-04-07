# Manuale Utente

Manuale operativo di ICRONIUM RR Bridge.

## Accesso
Apri il browser su:
- [http://localhost:8087](http://localhost:8087)

Home:
- `Login a RR online`
- `Login a RR locale`
- `Login a TZero`

## Login
### RR online
- usa credenziali Race Result remote
- le credenziali vengono ricordate nel browser
- la rotellina `settings` in alto a destra permette di cambiarle

### RR locale
- usa Race Result locale
- non richiede credenziali RR

### TZero
- al primo accesso chiede la `TZero Root Folder`
- ai successivi usa quella già definita
- dalla `Race List` puoi cambiarla con il bottone dedicato

## Race List
Mostra l'elenco gare.

Comportamento:
- in modalità RR mostra le gare RR
- in modalità TZero mostra le sottocartelle gara della root TZero

## Toolbar comune
Nelle pagine operative trovi una navbar stabile.

Voci principali:
- `Open RR`
- `RR Live`
- `TP View`
- `File View`
- `Reader View`
- `Settings`
- `Race List`

Note:
- il bottone corrente è evidenziato
- in `TZERO` il bottone `Open RR` sparisce
- in `TZERO` il bottone `RR Live` sparisce

## Settings
La pagina `Settings` è la vecchia `Race Detail`.

Qui configuri:
- i file della gara
- i timing point
- la blacklist
- i filtri orari sui file
- i file simulati

## Sync iniziale
Entrando in `Settings` viene eseguito `SYNC`.

### In RR
Carica:
- timing point
- tabella `BIB-CHIP`
- anagrafica atleta

### In TZero
Carica:
- `participants.csv`
- i file `file-*` nella cartella `download`
- i timing point da `config/timingpoints.json`
- le sorgenti censite da `config/sources.json`

Esclusioni automatiche file TZero:
- nomi contenenti `MERGED`
- nomi contenenti `CORREZIONI`

## Configurazione file
Da `Settings` puoi aggiungere e configurare file passaggi.

Campi principali:
- `Source`
- `Timing point`
- `Download every`
- `Sync offset`
- `From time`
- `To time`

I campi `From time` / `To time` escludono i rilevamenti fuori finestra alla radice del parsing.

Placeholder:
- `00:00:00`
- `23:59:00`

## Tipi di sorgente
Icone usate:
- `L`: file locale
- `N`: rete LAN
- `W`: URL WAN/remoto
- `S`: file simulato

## Simulated file
Da `Settings` puoi usare `Add simulated file`.

Configuri:
- nome file
- timing point
- finestra temporale da/a
- frequenza righe

Il file simulato:
- viene creato come `file-sim-*`
- in `TZERO` viene scritto nella cartella gara `download`
- in RR viene scritto sotto `~/icronium-work`

## Totals
Le celle principali sono:
- `READ`
- `SENT`
- `DISC`
- `UNIQUE`

In `TZERO`:
- `SENT` non compare
- il post verso RR non viene fatto davvero
- il processamento serve comunque a calcolare `Discarded`

## Last 10 cycles
Mostrano gli ultimi cicli della riga:
- `DL`
- `OUT`

In `TZERO`:
- `OUT` non compare

Le celle sono cliccabili e aprono il dettaglio del ciclo.

## Popup operative
### File content
- mostra il contenuto del file
- supporta filtro testuale
- puoi mettere in blacklist un codice

### Unique
- mostra i chip unici
- supporta filtro e ordinamento

### Discarded
- mostra i passaggi non riconosciuti
- da qui puoi blacklistare

### Blacklist
- mostra i codici esclusi
- `Restore` li riattiva

## File View
Vista per singolo file/source.

Mostra:
- file
- status
- totals
- colonna `Last 10 cycles`
- grafico minuto per minuto

Funzioni utili:
- `Start time` con `Apply` e `Reset`
- selezione `Visible files`
- ordinamento manuale con frecce
- ordine e filtri ricordati nel browser

In `TZERO`:
- niente `SENT`
- niente `OUT`

## TP View
Vista per timing point, a card.

Ogni card mostra:
- nome TP
- ultimi 10 passaggi unici
- totals aggregati
- speaker
- QR
- grafico del TP

Colore header card:
- verde: tutti i file running
- giallo: situazione mista / errore
- rosso: tutti stoppati

Funzioni utili:
- gestione `Visible timing points`
- ordinamento con frecce
- QR per la pagina speaker

## Speaker page
Pagina pubblica tokenizzata per un timing point.

Caratteristiche:
- non richiede login se hai il token
- è la versione full-screen della card TP
- mostra gli ultimi 10 passaggi unici
- mostra `Team`
- in `TZERO` nasconde `SENT`

## Reader View
Vista a card per reader/device.

Reader configurabili:
- `name`
- `location`: `LAN`, `WAN`, `FS`
- `type`: `ICRON`, `UBIDIUM`, `FEIBOT`

La card mostra:
- nome reader
- lista file odierni
- dimensione file
- orario file
- stato collegamento alla gara

Note:
- testo file verde se il nome file è già collegato alla gara
- clic sul nome file: apre il contenuto con filtro full text
- da lì puoi usare `Add to race`

### Scan LAN
- cerca reader sulla subnet `192.168.x.*`
- prova `http://<ip>/files`
- se risponde, censisce automaticamente il reader come `LAN / ICRON`

### Scan WAN
- trova i reader WAN attivi con file odierno
- apre una dialog di scelta
- il censimento non è automatico

### Add to race da reader
Quando aggiungi un file reader alla gara:
- `WAN` usa URL file remoto
- `LAN` usa URL file remoto LAN
- `FS` usa path locale

In `TZERO`:
- il file remoto continua a essere letto dalla sua URL
- lo scheduler ne salva anche una copia aggiornata in `download/`
- la sorgente resta persistita in `config/sources.json`

## TZero dietro le quinte
Per ogni gara TZero contano tre aree:
- `participants.csv`
- `download/`
- `config/`

In `config/` trovi:
- `timingpoints.json`
- `sources.json`

Quando cancelli un file da `Settings` in `TZERO`:
- non viene eliminato definitivamente
- viene spostato in `deleted/`
- viene rinominato con timestamp

## RR Live
Vista separata per il flusso live RR.

Azioni:
- `PLAY`
- `STOP`
- `RESET`
- `DELETE`

Nota:
- in `TZERO` questa vista non è disponibile dalla navbar

## Messaggi e log
Ogni pagina operativa ha la sezione `Messages`.

Serve per vedere:
- download
- upload
- errori
- discovery reader
- stato scheduler

Log completo:
- `logs/connector-rr.log`

## Suggerimento operativo minimo
1. scegli modalità login
2. apri la gara dalla `Race List`
3. fai `SYNC`
4. verifica anagrafica e TP
5. configura file o reader
6. avvia con `PLAY`
7. monitora `Totals`, `Last 10 cycles`, grafici e `Messages`
