package com.example.maliciousurldetector;

import java.util.HashSet;
import java.util.Set;

public class ScannedUrlStore {
    private static final Set<String> scannedMaliciousUrls = new HashSet<>();

    public static boolean isAlreadyScanned(String url) {
        // NOTE: We are not blocking re-scan. This just tells if it was ever scanned.
        return scannedMaliciousUrls.contains(url);
    }

    public static void addMaliciousUrl(String url) {
        scannedMaliciousUrls.add(url);
    }

    public static void clear() {
        scannedMaliciousUrls.clear();
    }
}
