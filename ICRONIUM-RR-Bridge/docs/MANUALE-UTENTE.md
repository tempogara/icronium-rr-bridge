# Manuale Utente

Questo manuale descrive il funzionamento operativo dell'applicazione ICRONIUM RR Bridge dal punto di vista dell'utente.

## Accesso all'applicazione
Apri il browser su:
- [http://localhost:8087](http://localhost:8087)

La home presenta due possibilità:
- `Login to RR`
- `Login to RR local`

## Login
### Login to RR
Usa questa modalità quando lavori con Race Result remoto.

Inserisci:
- `userid`
- `password`

Premi:
- `Login to RR`

Le credenziali vengono ricordate dal browser e riproposte agli accessi successivi.

### Login to RR local
Usa questa modalità quando lavori contro una installazione locale di Race Result.

Non servono credenziali RR.

Premi:
- `Login to RR local`

## Dashboard
Dopo il login si apre la dashboard.

La tabella mostra:
- `ID`
- `Event name`
- `Event date`

Le gare sono ordinate per data decrescente.

Per entrare in una gara:
- clicca `Select`

In basso è sempre presente l'area `Messages`, che mostra messaggi ricevuti via WebSocket.

## Race Detail
La pagina `Race Detail` è la vista principale di lavoro sui file di passaggi.

### Parte alta pagina
Nella toolbar trovi:
- `Dashboard`
- `Open RR`
- `RR Live`
- `TP View`
- `RR ID`
- nome evento
- data evento

Sotto la toolbar vedi il nome gara e la data evento.

## Sync iniziale
Quando entri nella pagina viene eseguito automaticamente un `SYNC`.

Il sync:
- carica da RR i `Timing Point`
- carica da RR la tabella `BIB-CHIP`
- salva i dati in sessione

Puoi rilanciare manualmente il sync con il bottone:
- `SYNC`

## Popup di supporto
### BIB-CHIP
Mostra i dati atleta caricati da RR:
- `Chip`
- `Bib`
- `LastName`
- `FirstName`

### Timing Points
Mostra tutti i timing point disponibili per la gara.

### Blacklist
Mostra i codici esclusi manualmente e permette il `Restore`.

### Unknown chips
Mostra i chip non riconosciuti rispetto alla tabella BIB-CHIP.

## Aggiunta di un source
Per aggiungere una sorgente di passaggi usa:
- `Add file passings`

Nella popup puoi indicare:
- `Source`: URL o percorso file locale
- `Timing point`
- `Download every`: ogni quanti secondi scaricare
- `Sync`: eventuale offset da applicare al timestamp prima dell'invio esterno

Esempi di source:
- file locale
- URL interno di rete `192.168...`
- URL remoto HTTP/HTTPS

## Tabella dei source
Ogni riga della tabella rappresenta un source.

### Colonna Actions
Bottoni disponibili sulla singola riga:
- `Play`
- `Stop`
- `Rewind`
- `Edit`
- `Duplicate`
- `Delete`

Regole:
- all'inizio le righe sono in stato `STOPPED`
- se la riga è `RUNNING`, restano attivi solo i comandi compatibili con l'esecuzione
- `STOP` diventa disponibile solo quando la riga è in esecuzione

### Colonna Status
Stati possibili:
- `STOPPED`
- `RUNNING`
- `ERROR`

Se un download fallisce, la riga va in `ERROR`, ma continua a riprovare ai cicli successivi finché non premi `STOP`.

### Colonna Source
Mostra:
- icona del tipo di sorgente
- nome file
- intervallo `every`
- eventuale `sync`

Significato icone:
- `L`: file locale
- `N`: rete locale
- `W`: web/remoto

Cliccando sul nome file apri la popup `File content`.

### Colonna Last download
Mostra:
- data ultimo download
- ora ultimo download
- `elapsed`, cioè quanto tempo è passato dall'ultimo download

### Colonna Totals
Mostra 4 celle:
- `READ`
- `SENT`
- `DISC`
- `UNIQUE`

Significato:
- `READ`: righe lette dal file
- `SENT`: righe inviate al sistema esterno
- `DISC`: righe scartate
- `UNIQUE`: chip unici letti

Colori:
- `READ` verde se maggiore di zero
- `SENT` verde se tutto inviato, giallo se parziale, rosso se zero
- `DISC` rosso se ci sono scarti
- `UNIQUE` blu

Interazione:
- clic su `READ`: apre il dettaglio letture
- clic su `DISC`: apre il dettaglio scarti
- clic su `UNIQUE`: apre il dettaglio chip unici

### Colonna Last 10 cycles
Mostra gli ultimi 10 cicli di lavoro della riga:
- `DL`: quante righe sono state lette in quel ciclo
- `OUT`: quante righe sono state inviate in quel ciclo

Colori:
- `DL` verde se il ciclo ha letto righe
- `OUT` verde se invio completo
- `OUT` giallo se invio parziale

Cliccando su una cella apri il dettaglio del singolo ciclo.

## Popup File content
Mostra il contenuto del file selezionato.

Caratteristiche:
- sottotitolo con URL o path completo
- ordine iniziale dal più recente al più vecchio
- pulsante `Newest/Oldest`
- campo filtro
- colonna atleta
- bottone `Black list` per escludere un codice

Se metti un codice in blacklist:
- sparisce dalla lista
- smette di essere conteggiato
- smette di essere inviato

## Popup Unique
Mostra i chip unici letti.

Caratteristiche:
- ordinamento `Newest/Oldest`
- filtro testuale
- visualizzazione atleta associato

In `TP View` vedi anche il file di provenienza.

## Popup Discarded
Mostra le righe lette ma non riconosciute rispetto alla tabella `BIB-CHIP`.

Anche da qui puoi aggiungere un codice alla blacklist.

Quando blacklisti un codice:
- sparisce dalla popup
- diminuisce il conteggio `DISC`

## Blacklist
La blacklist è per-riga.

Questo significa che un codice escluso su un source non viene automaticamente escluso sugli altri source.

La popup `Blacklist` in toolbar permette di:
- vedere i codici esclusi
- ripristinarli con `Restore`

## Azioni globali
Nella tabella puoi eseguire azioni su tutte le righe insieme:
- `PLAY ALL`
- `STOP ALL`
- `REWIND ALL`
- `DELETE ALL`

Usale quando vuoi governare tutti i source in blocco.

## TP View
La pagina `TP View` raggruppa i source per timing point.

Per ogni timing point vedi:
- nome timing point
- lista dei source associati con stato di ogni file
- box `Totals`

Il box `Totals` mostra:
- `READ`
- `SENT`
- `DISC`
- `UNIQUE`

Interazioni:
- clic su `READ`: popup con tutte le letture aggregate del timing point
- clic su `UNIQUE`: popup con i chip unici aggregati del timing point

Le popup hanno:
- ordinamento `Newest/Oldest`
- campo filtro
- in più, nella vista aggregata, il nome del file di provenienza

## RR Live
La pagina `RR Live` gestisce un altro flusso operativo, basato sulle API live di Race Result.

Quando entri nella pagina:
- la sync iniziale carica automaticamente la lista delle API disponibili
- la tabella viene popolata

Azioni disponibili:
- `PLAY`
- `STOP`
- `RESET`
- `DELETE`
- `Global actions`

`RESET` rimuove la memoria dell'ultimo file spedito. Al successivo `PLAY` il flusso riparte da zero.

Anche qui è presente l'area `Messages` richiudibile.

## Open RR
Il bottone `Open RR` apre Race Result.

Comportamento:
- in locale apre la gara locale con `pw=0`
- in remoto apre l'URL gestito dal backend

## Messaggi
L'area `Messages` è presente nelle pagine operative.

Serve per monitorare:
- download
- upload
- errori
- messaggi di sistema
- attività scheduler

La sezione è richiudibile verso il basso e ricorda lo stato nel browser.

## Log applicativo
I log completi sono scritti in:
- [logs/connector-rr.log](/Users/vitocandela/Documents/Playground/ICRONIUM-RR-Bridge/logs/connector-rr.log)

## Suggerimenti operativi
### Per iniziare una gara
1. fai login
2. seleziona la gara in dashboard
3. verifica `SYNC`
4. controlla `BIB-CHIP` e `Timing Points`
5. aggiungi i source necessari
6. premi `PLAY`
7. monitora `Totals`, `Last 10 cycles` e `Messages`

### Se un file dà errore
1. controlla il path o l'URL
2. guarda lo stato `ERROR`
3. verifica i messaggi in basso
4. usa `STOP` solo se vuoi fermare i tentativi automatici

### Se vuoi ripartire da zero su un source
1. premi `STOP`
2. premi `REWIND`
3. premi di nuovo `PLAY`

### Se vedi troppi scarti
1. apri `DISC`
2. controlla i codici non riconosciuti
3. verifica `BIB-CHIP`
4. usa la blacklist solo per i codici che vuoi ignorare davvero
