# Start Here - Distribuzione ICRONIUM RR Bridge

Guida rapida per chi riceve il pacchetto distribuito.

## Contenuto del pacchetto
Nel file zip trovi:
- `app.jar`
- `start.sh` e `stop.sh`
- `start.bat` e `stop.bat`
- `config/application.yml`
- `docs/`
- `logs/`
- `data/`

## Requisiti minimi
- Java `21`
- Browser moderno
- rete disponibile verso Race Result o verso le cartelle/TP usati

## Primo avvio
1. scompatta il file zip
2. entra nella cartella estratta
3. verifica Java:

```bash
java -version
```

## Avvio su macOS/Linux
```bash
chmod +x start.sh stop.sh
./start.sh
```

## Avvio su Windows
- doppio click su `start.bat`

## Accesso all'applicazione
Di default apri:
- `http://localhost:8089`

La porta del pacchetto distribuito dipende da:
- `config/application.yml`

## Modalità di login
Home:
- `Login a RR online`
- `Login a RR locale`
- `Login a TZero`

### RR online
- usa credenziali Race Result remote
- il browser può ricordarle

### RR locale
- usa Race Result locale
- non richiede credenziali RR

### TZero
- al primo accesso chiede la `TZero Root Folder`
- poi la riusa automaticamente

## Dove leggere i manuali
Nel pacchetto trovi:
- `docs/start-here-distribuzione.html`
- `docs/manuale-utente.html`
- `docs/manuale-tecnico.html`
- `docs/MANUALE-UTENTE.md`

Per partire apri:
- `docs/start-here-distribuzione.html`

## Dove trovare i log
Runtime pacchetto:
- `logs/app.out`

Log applicativo interno:
- `logs/connector-rr.log` se configurato dall'applicazione

## Dove vengono salvati i dati
Cartella:
- `data/`

Qui l'app salva lo stato persistente dell'applicazione.

## Arresto applicazione
### macOS/Linux
```bash
./stop.sh
```

### Windows
- usa `stop.bat` oppure arresta il processo Java dal Task Manager

## Problemi comuni
### La pagina non si apre
Verifica:
- che il processo Java sia partito
- che la porta configurata sia quella giusta
- che `logs/app.out` non contenga errori

### Java non trovato
Installa Java 21 e riprova.

### Porta occupata
Cambia `config/application.yml` e riavvia.

## Riferimenti rapidi
- avvio: `start.sh` o `start.bat`
- stop: `stop.sh`
- URL: `http://localhost:8089`
- manuale utente: `docs/manuale-utente.html`
- log: `logs/app.out`
