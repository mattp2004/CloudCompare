package org.cloudcompare.backend.util;

import java.time.Instant;
import java.util.Date;

public class Logger {
    public static void Log(String text){
        System.out.println("(" +Date.from(Instant.now()).toString() + ") "+ text);
    }
    public static void LogError(String text) {
        System.out.println("[ERROR] + (" + Date.from(Instant.now()).toString() + ")" + text);
    }
}