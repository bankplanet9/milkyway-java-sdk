package com.bankplanet9.milkyway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A localhost {@link HttpServer} stub for tests — no external WireMock, no live
 * network. Register per-path handlers (or scripted sequences) and assert on
 * captured requests.
 */
final class StubServer implements AutoCloseable {

    /** A captured inbound request. */
    static final class Captured {
        final String method;
        final String path;
        final String query;
        final Map<String, List<String>> headers;
        final String body;

        Captured(String method, String path, String query, Map<String, List<String>> headers, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.headers = headers;
            this.body = body;
        }

        String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    /** A canned response. */
    static final class Response {
        final int status;
        final String body;
        final String contentType;

        Response(int status, String body) {
            this(status, body, "application/json");
        }

        Response(int status, String body, String contentType) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
        }
    }

    private final HttpServer server;
    private final List<Captured> captured = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> hitCounts = new ConcurrentHashMap<>();

    StubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    /** Registers a handler that returns a fixed response for a path. */
    void onPath(String path, Function<Captured, Response> responder) {
        server.createContext(path, exchange -> {
            Captured c = capture(exchange);
            captured.add(c);
            hitCounts.computeIfAbsent(path, k -> new AtomicInteger()).incrementAndGet();
            Response r = responder.apply(c);
            write(exchange, r);
        });
    }

    /**
     * Registers a scripted sequence of responses for a path: the Nth hit returns the
     * Nth response; the last response repeats once the script is exhausted.
     */
    void onPathSequence(String path, List<Response> responses) {
        List<Response> seq = new ArrayList<>(responses);
        AtomicInteger idx = new AtomicInteger();
        server.createContext(path, exchange -> {
            Captured c = capture(exchange);
            captured.add(c);
            hitCounts.computeIfAbsent(path, k -> new AtomicInteger()).incrementAndGet();
            int i = idx.getAndIncrement();
            Response r = seq.get(Math.min(i, seq.size() - 1));
            write(exchange, r);
        });
    }

    private static Captured capture(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new Captured(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders(),
                body);
    }

    private static void write(HttpExchange exchange, Response r) throws IOException {
        byte[] bytes = r.body == null ? new byte[0] : r.body.getBytes(StandardCharsets.UTF_8);
        if (r.contentType != null) {
            exchange.getResponseHeaders().add("Content-Type", r.contentType);
        }
        exchange.sendResponseHeaders(r.status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.close();
        }
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String url(String path) {
        return baseUrl() + path;
    }

    List<Captured> captured() {
        return captured;
    }

    int hits(String path) {
        AtomicInteger c = hitCounts.get(path);
        return c == null ? 0 : c.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
