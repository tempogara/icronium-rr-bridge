package it.icron.rfid;

import com.payne.connect.net.NetworkHandle;
import com.payne.reader.Reader;
import com.payne.reader.base.BaseInventory;
import com.payne.reader.base.Consumer;
import com.payne.reader.bean.config.AntennaCount;
import com.payne.reader.bean.config.Session;
import com.payne.reader.bean.config.Target;
import com.payne.reader.bean.receive.Failure;
import com.payne.reader.bean.receive.InventoryFailure;
import com.payne.reader.bean.receive.InventoryTag;
import com.payne.reader.bean.receive.InventoryTagEnd;
import com.payne.reader.bean.receive.Success;
import com.payne.reader.bean.send.CustomSessionTargetInventory;
import com.payne.reader.bean.send.InventoryConfig;
import com.payne.reader.bean.send.InventoryParam;
import com.payne.reader.process.ReaderImpl;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UhfReaderApp {
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withLocale(Locale.ITALY)
                    .withZone(ZoneId.systemDefault());

    private final AppConfig config;
    private final InventoryParam inventoryParam = new InventoryParam();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private Reader reader;

    public UhfReaderApp(AppConfig config) {
        this.config = config;
    }

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.parse(args);
            UhfReaderApp app = new UhfReaderApp(config);
            app.run();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Errore fatale: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private void run() throws InterruptedException {
        configureInventoryParam();

        reader = ReaderImpl.create(config.antennaCount);
        boolean connected = reader.connect(new NetworkHandle(config.host, config.tcpPort));
        if (!connected) {
            throw new IllegalStateException("Impossibile connettersi al reader TCP " + config.host + ":" + config.tcpPort);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "rfid-shutdown"));

        configureCallbacks();
        queryVersion();
        setOutputPower();

        System.out.println("Reader connesso. TCP host=" + config.host
                + " port=" + config.tcpPort
                + " power=" + config.power
                + " antennas=" + config.antennas);

        startInventory();

        while (running.get()) {
            Thread.sleep(500);
        }
    }

    private void configureInventoryParam() {
        inventoryParam.setAntennaCount(config.antennaCount);
        inventoryParam.setSession(config.session);
        inventoryParam.setTarget(config.target);
        inventoryParam.setRepeat((byte) 1);
        inventoryParam.setDelayMs(0);
        inventoryParam.setLoopCount(-1);
        inventoryParam.clearCustomSessionIds();
        for (int antennaId : config.antennas) {
            inventoryParam.addCustomSessionId(antennaId);
        }
    }

    private void configureCallbacks() {
        BaseInventory inventory = new CustomSessionTargetInventory.Builder()
                .session(config.session)
                .target(config.target)
                .enablePhase(config.enablePhase)
                .repeat((byte) 1)
                .build();

        InventoryConfig inventoryConfig = new InventoryConfig.Builder()
                .setInventoryParam(inventoryParam)
                .setInventory(inventory)
                .setOnInventoryTagSuccess(this::onTagRead)
                .setOnInventoryTagEndSuccess(this::onInventoryEnd)
                .setOnFailure(this::onInventoryFailure)
                .build();
        reader.setInventoryConfig(inventoryConfig);
    }

    private void queryVersion() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        reader.getFirmwareVersion(version -> {
            System.out.println("Firmware: " + version.getChipType() + " V" + version.getVersion());
            latch.countDown();
        }, failure -> {
            System.err.println("Lettura firmware fallita: " + Failure.getNameForResultCode(failure.getErrorCode()));
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    private void setOutputPower() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        reader.setOutputPowerUniformly(config.power, success -> {
            System.out.println("Potenza impostata a " + config.power);
            latch.countDown();
        }, failure -> {
            System.err.println("Impostazione potenza fallita: " + Failure.getNameForResultCode(failure.getErrorCode()));
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    private void startInventory() {
        doNextAntenna(false);
    }

    private void doNextAntenna(boolean nextAntenna) {
        if (!running.get()) {
            return;
        }

        int antennaId = inventoryParam.getAntennaId(nextAntenna);
        reader.setWorkAntenna(antennaId, successAntConsumer, failureAntConsumer);
    }

    private final Consumer<Success> successAntConsumer = success -> reader.startInventory();
    private final Consumer<Failure> failureAntConsumer = failure -> {
        System.err.println("Set antenna fallita: " + Failure.getNameForResultCode(failure.getErrorCode()));
        doNextAntenna(true);
    };

    private void onTagRead(InventoryTag tag) {
        System.out.println(TS_FORMAT.format(Instant.now()) + " EPC=" + tag.getEpc());
    }

    private void onInventoryEnd(InventoryTagEnd tagEnd) {
        if (!running.get()) {
            return;
        }

        if (tagEnd.isFinished()) {
            shutdown();
            return;
        }

        doNextAntenna(true);
    }

    private void onInventoryFailure(InventoryFailure failure) {
        System.err.println("Inventory failure ant=" + failure.getAntId()
                + " code=" + Failure.getNameForResultCode(failure.getErrorCode()));
        doNextAntenna(true);
    }

    private void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            if (reader != null) {
                try {
                    reader.stopInventory();
                } catch (Exception ignored) {
                }
                reader.disconnect();
            }
        } finally {
            System.out.println("Reader disconnesso.");
        }
    }

    private static void printUsage() {
        System.out.println("Uso:");
        System.out.println("  zsh run-reader.sh --host <ip> --tcp-port <porta> [opzioni]");
        System.out.println("Opzioni:");
        System.out.println("  --host <ip>");
        System.out.println("  --tcp-port <porta>");
        System.out.println("  --power <1-33>");
        System.out.println("  --antennas <0,1,...>");
        System.out.println("  --antenna-count <1|4|8|16>");
        System.out.println("  --session <S0|S1|S2|S3>");
        System.out.println("  --target <A|B>");
        System.out.println("  --phase");
    }

    private static final class AppConfig {
        private String host;
        private int tcpPort = 4001;
        private byte power = 33;
        private List<Integer> antennas = List.of(0);
        private AntennaCount antennaCount = AntennaCount.SINGLE_CHANNEL;
        private Session session = Session.S0;
        private Target target = Target.A;
        private boolean enablePhase;

        private static AppConfig parse(String[] args) {
            AppConfig config = new AppConfig();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--host" -> config.host = requireValue(args, ++i, "--host");
                    case "--tcp-port" -> config.tcpPort = Integer.parseInt(requireValue(args, ++i, "--tcp-port"));
                    case "--power" -> config.power = Byte.parseByte(requireValue(args, ++i, "--power"));
                    case "--antennas" -> config.antennas = parseAntennaList(requireValue(args, ++i, "--antennas"));
                    case "--antenna-count" -> config.antennaCount = parseAntennaCount(requireValue(args, ++i, "--antenna-count"));
                    case "--session" -> config.session = Session.valueOf(requireValue(args, ++i, "--session").toUpperCase(Locale.ROOT));
                    case "--target" -> config.target = Target.valueOf(requireValue(args, ++i, "--target").toUpperCase(Locale.ROOT));
                    case "--phase" -> config.enablePhase = true;
                    case "--help", "-h" -> throw new IllegalArgumentException("Richiesta help");
                    default -> throw new IllegalArgumentException("Parametro non riconosciuto: " + arg);
                }
            }

            if (config.host == null || config.host.isBlank()) {
                throw new IllegalArgumentException("Manca --host");
            }
            if (config.tcpPort < 1 || config.tcpPort > 65535) {
                throw new IllegalArgumentException("--tcp-port deve essere tra 1 e 65535");
            }
            if (config.power < 1 || config.power > 33) {
                throw new IllegalArgumentException("--power deve essere tra 1 e 33");
            }
            validateAntennas(config.antennas, config.antennaCount);
            return config;
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Manca un valore per " + option);
            }
            return args[index];
        }

        private static List<Integer> parseAntennaList(String value) {
            String[] tokens = value.split(",");
            List<Integer> result = new ArrayList<>(tokens.length);
            for (String token : tokens) {
                result.add(Integer.parseInt(token.trim()));
            }
            return result;
        }

        private static AntennaCount parseAntennaCount(String value) {
            return switch (value) {
                case "1" -> AntennaCount.SINGLE_CHANNEL;
                case "4" -> AntennaCount.FOUR_CHANNELS;
                case "8" -> AntennaCount.EIGHT_CHANNELS;
                case "16" -> AntennaCount.SIXTEEN_CHANNELS;
                default -> throw new IllegalArgumentException("--antenna-count deve essere 1, 4, 8 o 16");
            };
        }

        private static void validateAntennas(List<Integer> antennas, AntennaCount antennaCount) {
            int max = switch (antennaCount) {
                case SINGLE_CHANNEL -> 0;
                case FOUR_CHANNELS -> 3;
                case EIGHT_CHANNELS -> 7;
                case SIXTEEN_CHANNELS -> 15;
            };

            for (int antenna : antennas) {
                if (antenna < 0 || antenna > max) {
                    throw new InvalidParameterException("Antenna fuori range per antenna-count=" + antennaCount + ": " + antenna);
                }
            }
        }
    }
}
