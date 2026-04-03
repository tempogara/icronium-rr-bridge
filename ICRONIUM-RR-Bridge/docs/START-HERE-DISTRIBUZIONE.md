# Start Here - Distribuzione ICRONIUM RR Bridge

Questa guida è pensata per chi riceve il pacchetto distribuito dell'applicazione e deve avviarla senza conoscere il progetto sorgente.

## Contenuto del pacchetto
Nel file zip trovi:
- `app.jar`: applicazione principale
- `start.sh` e `stop.sh`: avvio/arresto su macOS/Linux
- `start.bat` e `stop.bat`: avvio/arresto su Windows
- `config/application.yml`: configurazione runtime
- `logs/`: cartella log
- `data/`: cartella dati persistenti
- `docs/`: manuali

## Requisiti minimi
- Java `21` installato
- Browser moderno
- Accesso alla rete necessaria per collegarsi a Race Result e agli endpoint esterni

## Primo avvio
1. Scompatta il file zip in una cartella a tua scelta
2. Apri la cartella estratta
3. Verifica di avere Java 21 disponibile con:

```bash
java -version
```

4. Avvia l'applicazione

### Avvio su macOS/Linux
Apri il terminale nella cartella estratta e lancia:

```bash
chmod +x start.sh stop.sh
./start.sh
```

### Avvio su Windows
Doppio clic su:
- `start.bat`

## Accesso all'applicazione
Dopo l'avvio apri il browser su:
- `http://localhost:8089`

Nota:
- nel pacchetto distribuito la porta di default è configurata in `config/application.yml`
- se modifichi la porta nel file di configurazione, dovrai usare la nuova porta nel browser

## Login
Nella pagina iniziale trovi due modalità:
- `Login to RR`
- `Login to RR local`

Usa `Login to RR local` se lavori contro un ambiente Race Result locale.

## Dove leggere i manuali
Nel pacchetto trovi:
- `docs/manuale-utente.html`
- `docs/manuale-tecnico.html`
- `docs/MANUALE-UTENTE.md`

Il file più adatto per partire è:
- `docs/manuale-utente.html`

## Dove trovare i log
I log runtime del pacchetto distribuito vengono scritti in:
- `logs/app.out`

## Dove vengono salvati i dati
La cartella dati del pacchetto distribuito è:
- `data/`

Qui l'applicazione può memorizzare configurazioni, stato e dati persistenti in base alle funzioni attivate.

## Arresto applicazione
### macOS/Linux
Da terminale:

```bash
./stop.sh
```

### Windows
Il file `stop.bat` è solo informativo. Se necessario, arresta il processo Java dal Task Manager oppure usa un comando `taskkill`.

## Configurazione porta
File:
- `config/application.yml`

Esempio:

```yaml
server:
  port: 8089
```

## Problemi comuni
### Il browser non apre la pagina
Verifica:
- che il processo Java sia partito correttamente
- che la porta configurata sia quella corretta
- che nel file `logs/app.out` non ci siano errori di avvio

### Java non trovato
Installa Java 21 e riprova.

### La porta è occupata
Modifica `config/application.yml` e imposta una porta libera, poi riavvia l'applicazione.

## Riferimenti rapidi
- Avvio: `start.sh` o `start.bat`
- Stop: `stop.sh`
- URL applicazione: `http://localhost:8089`
- Manuale utente: `docs/manuale-utente.html`
- Log: `logs/app.out`
