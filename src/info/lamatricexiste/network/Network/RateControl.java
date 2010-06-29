/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class RateControl {

    // TODO: Calculate a rounded up value from experiments in different networks
    private final String TAG = "RateControl";
    private final int REACH_TIMEOUT = 5000;
    public String[] indicator;
    public int rate = 800; // Slow start
    public boolean is_indicator_discovered = false;

    public void adaptRate() {
        int response_time = 0;
        // TODO: Use an indicator with a port, calculate java round trip time
        // if (indicator.length > 1) {
        // Log.v(TAG, "use a socket here, port=" + getIndicator()[1]);
        // } else {
        is_indicator_discovered = true;
        if ((response_time = getAvgResponseTime(indicator[0], 3)) > 0) {
            if (response_time > 100) { // Most distanced hosts
                rate = response_time * 5; // Minimum 500ms
            } else {
                rate = response_time * 10; // Maximum 1000ms
            }
            Log.v(TAG, "adapt=" + response_time + "ms -> " + rate + "ms");
        }
        // }
    }

    private int getAvgResponseTime(String host, int count) {
        try {
            String cmd = "/system/bin/ping";
            if ((new File(cmd)).exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec(cmd + " -q -n -W 2 -c " + count + " " + host);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), 1);
                while ((line = r.readLine()) != null) {
                    // Log.d("    ", line);
                    // rtt min/avg/max/mdev = 1.281/2.693/4.939/1.605 ms
                    matcher = Pattern
                            .compile(
                                    "^rtt min\\/avg\\/max\\/mdev = [0-9\\.]+\\/[0-9\\.]+\\/([0-9\\.]+)\\/[0-9\\.]+ ms$")
                            .matcher(line);
                    if (matcher.matches()) {
                        return (int) Float.parseFloat(matcher.group(1));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't use native ping: " + e.getMessage());
            try {
                final long start = System.nanoTime();
                if (InetAddress.getByName(host).isReachable(REACH_TIMEOUT)) {
                    Log.i(TAG, "Using Java ICMP request instead ...");
                    return (int) ((System.nanoTime() - start) / 1000);
                }
            } catch (Exception e1) {
                Log.e(TAG, e1.getMessage());
            }
        }
        return rate;
    }
}
