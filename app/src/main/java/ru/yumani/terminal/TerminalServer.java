package ru.yumani.terminal;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class TerminalServer {
    public interface Listener {
        void onAmount(String amount);
        void onCancel();
        void onClient(boolean connected);
    }
    private static ServerSocket server;
    private static volatile boolean running = false;
    private static Listener listener;

    public static void start(Listener l) {
        listener = l;
        running = true;
        new Thread(TerminalServer::runServer, "tcp-server").start();
        new Thread(TerminalServer::runBeacon, "udp-beacon").start();
    }

    public static void stop() { running = false; }

    private static void runBeacon() {
        try (DatagramSocket s = new DatagramSocket()) {
            s.setBroadcast(true);
            while (running) {
                byte[] msg = ("YUMANI_HERE|" + TerminalState.code).getBytes(StandardCharsets.UTF_8);
                for (String host : new String[]{"255.255.255.255", "192.168.1.255", "192.168.0.255"}) {
                    try {
                        s.send(new DatagramPacket(msg, msg.length,
                                InetAddress.getByName(host), 8091));
                    } catch (Exception ignored) {}
                }
                Thread.sleep(2000);
            }
        } catch (Exception ignored) {}
    }

    private static void runServer() {
        try (ServerSocket ss = new ServerSocket(8090)) {
            while (running) {
                Socket sock = ss.accept();
                new Thread(() -> handle(sock)).start();
            }
        } catch (Exception ignored) {}
    }

    private static void handle(Socket sock) {
        try (sock; BufferedReader in = new BufferedReader(
                new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8))) {
            sock.setSoTimeout(0);
            String line = in.readLine();
            if (line == null) return;
            if (!("CODE " + TerminalState.code).equals(line.trim())) {
                out.write("BAD\n"); out.flush(); return;
            }
            out.write("OK\n"); out.flush();
            final boolean[] alive = {true};
            if (listener != null) listener.onClient(true);
            while (running && alive[0]) {
                String cmd = in.readLine();
                if (cmd == null) break;
                cmd = cmd.trim();
                if (cmd.startsWith("AMOUNT")) {
                    String a = cmd.length() > 7 ? cmd.substring(7).trim() : "0.00";
                    TerminalState.amount = a;
                    TerminalState.status = "SHOWING";
                    if (listener != null) listener.onAmount(a);
                    out.write("OK\n");
                } else if (cmd.equals("CANCEL")) {
                    TerminalState.status = "READY";
                    if (listener != null) listener.onCancel();
                    out.write("OK\n");
                } else if (cmd.equals("STATUS")) {
                    String st = TerminalState.status;
                    if (st.equals("PAID")) st = "PAID " + TerminalState.rrn;
                    out.write(st + "\n");
                } else if (cmd.equals("PING")) {
                    out.write("OK\n");
                } else if (cmd.equals("BYE")) {
                    out.write("OK\n");
                    break;
                } else {
                    out.write("ERR\n");
                }
                out.flush();
            }
        } catch (Exception ignored) {}
        if (listener != null) listener.onClient(false);
    }

    public static void markPaid() {
        TerminalState.rrn = String.format("%012d", (long) (Math.random() * 1e12));
        TerminalState.status = "PAID";
    }
    public static void markDeclined() { TerminalState.status = "DECLINED"; }
    public static void reset() { TerminalState.status = "READY"; }
}
