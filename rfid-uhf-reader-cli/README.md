# RFID UHF Reader CLI

CLI Java minimale per leggere tag EPC da un lettore RFID UHF via TCP socket usando lo SDK del vendor.
Stampa a video solo `timestamp + EPC`.

## Requisiti

- Java 21
- Lettore raggiungibile via TCP
- JAR vendor gia inclusi in `libs/`

## Avvio

```bash
cd /Users/vitocandela/Documents/Playground/rfid-uhf-reader-cli
GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew run --args="--host 192.168.1.200 --tcp-port 4001"
```

## Output

```text
2026-04-08 15:42:10.123 EPC=E2000017221101441890ABCD
2026-04-08 15:42:10.451 EPC=E2000017221101441890ABCD
```

## Opzioni utili

```text
--host <ip>              IP del lettore
--tcp-port <n>           Porta TCP, default 4001
--power <1-33>           Potenza RF, default 33
--antennas <id,id,...>   Antenne da ciclare, default 0
--antenna-count <1|4|8|16>
--session <S0|S1|S2|S3>  Default S0
--target <A|B>           Default A
--phase                  Abilita phase nel payload
```

## Esempio

```bash
GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew run --args="--host 192.168.1.200 --tcp-port 4001 --antenna-count 4 --antennas 0,1 --power 30"
```

## Alternativa

```bash
zsh run-reader.sh --host 192.168.1.200 --tcp-port 4001
```

Il wrapper Gradle scarica Gradle nella cartella locale `.gradle-user-home`.
Premi `Ctrl+C` per fermare la lettura.
