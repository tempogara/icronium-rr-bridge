# Spring Boot WebSocket Chat (Eclipse)

Progetto demo Spring Boot che comunica via WebSocket (STOMP) con una pagina HTML.

## Requisiti
- Java 17+

## Avvio da terminale
```bash
./gradlew bootRun
```

Apri: `http://localhost:8080/index.html`

## Import in Eclipse (consigliato)
1. `File -> Import -> Gradle -> Existing Gradle Project`
2. Seleziona la cartella `sprintboot-websocket-eclipse`
3. Finish

## Esecuzione in Eclipse
- Apri la classe `it.icron.icronium.connector.rr.ChatApplication`
- `Run As -> Java Application`

## Endpoint WebSocket
- Endpoint handshake: `/ws`
- Invio client -> server: `/app/chat.send`
- Broadcast server -> client: `/topic/messages`
