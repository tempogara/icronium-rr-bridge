package it.icron.icronium.connector.rr;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {

	

	
	
    private static final Path LOG_FILE =
       IcroniumApplication.WORK_DIR.resolve("icronium-connector.log");

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String msg) {

        try {

            String line =
                "[" + LocalDateTime.now().format(FMT) + "] "
                + msg + "\n";

            
            
            Files.writeString(
                LOG_FILE,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

        } catch (IOException ignored) {}
    }
    
    public static synchronized void logException(String msg, Exception ex) {

        try {

        	if(msg==null)
        		msg="";
            String line =
                "[" + LocalDateTime.now().format(FMT) + "] "
                + msg+" "+ex.getMessage() + ", segue stacktrace:\n";

            Files.writeString(
                LOG_FILE,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
            
            try(PrintStream printer=new PrintStream(new FileOutputStream(LOG_FILE.toFile(), true))){
            	ex.printStackTrace(printer);
            }
            
        } catch (IOException ignored) {}
    }
    
}
