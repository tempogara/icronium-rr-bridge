package it.icron.rfid;

import com.fazecast.jSerialComm.SerialPort;
import com.payne.connect.port.SerialPortHandle;
import com.payne.reader.base.Consumer;
import com.payne.reader.communication.ConnectHandle;
import com.payne.reader.util.LLLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JSerialPortConnectHandle implements ConnectHandle {
    private final byte[] dataBuffer = new byte[2048];
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final String portName;
    private final int baudRate;

    private volatile SerialPort serialPort;
    private volatile OutputStream outputStream;
    private volatile InputStream inputStream;
    private volatile Consumer<byte[]> consumer;

    public JSerialPortConnectHandle(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    @Override
    public boolean onConnect() {
        synchronized (this) {
            try {
                serialPort = SerialPort.getCommPort(portName);
                boolean open = serialPort.openPort();
                if (!open) {
                    return false;
                }

                serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                outputStream = serialPort.getOutputStream();
                inputStream = serialPort.getInputStream();
                running.set(true);
                connected.set(true);

                Thread receiveThread = new ReceiveThread();
                receiveThread.setName("rfid-read-" + SerialPortHandle.mTC.getAndIncrement());
                receiveThread.setDaemon(true);
                receiveThread.start();
            } catch (Exception e) {
                connected.set(false);
                System.err.println("Connessione seriale fallita: " + e.getMessage());
            }
            return connected.get();
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public boolean onSend(byte[] bytes) {
        synchronized (this) {
            try {
                outputStream.write(bytes);
                outputStream.flush();
                return true;
            } catch (Exception e) {
                connected.set(false);
                return false;
            }
        }
    }

    @Override
    public void onReceive(Consumer<byte[]> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onDisconnect() {
        synchronized (this) {
            running.set(false);
            connected.set(false);

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
                outputStream = null;
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
                inputStream = null;
            }

            if (serialPort != null) {
                serialPort.closePort();
                serialPort = null;
            }
        }
    }

    private final class ReceiveThread extends Thread {
        @Override
        public void run() {
            LLLog.i("JSerialPortConnectHandle receive thread started");
            while (running.get()) {
                if (!connected.get()) {
                    sleepQuietly(50);
                    continue;
                }

                try {
                    int available = inputStream.available();
                    if (available <= 0) {
                        sleepQuietly(20);
                        continue;
                    }

                    int readLen = inputStream.read(dataBuffer);
                    if (readLen <= 0) {
                        continue;
                    }

                    byte[] receiveBytes = new byte[readLen];
                    System.arraycopy(dataBuffer, 0, receiveBytes, 0, readLen);
                    if (consumer != null) {
                        consumer.accept(receiveBytes);
                    }
                } catch (IOException e) {
                    connected.set(false);
                    LLLog.w("Seriale disconnessa: " + e.getMessage());
                } catch (Exception e) {
                    LLLog.w("Errore thread seriale: " + e.getMessage());
                }
            }
            LLLog.w("JSerialPortConnectHandle receive thread finished");
        }

        private void sleepQuietly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
