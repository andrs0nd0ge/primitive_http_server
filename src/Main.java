import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    static String HTML_FILE_NAME = "index.html";
    static String CSS_FILE_NAME = "forms.css";
    static String BG_FILE_NAME = "bg.png";
    static String PICTURE_FILE_NAME = "1.jpg";
    static String UTIL_FOLDER = "src/util";
    static String BG_FOLDER = UTIL_FOLDER + "/bg";
    static String IMAGES_FOLDER = UTIL_FOLDER + "/images";
    static String CSS_FOLDER = UTIL_FOLDER + "/css";
    static Path HTML_FULL_PATH = Path.of(String.format("%s/%s", UTIL_FOLDER, HTML_FILE_NAME));
    static Path CSS_FULL_PATH = Path.of(String.format("%s/%s", CSS_FOLDER, CSS_FILE_NAME));
    static Path BG_FULL_PATH = Path.of(String.format("%s/%s", BG_FOLDER, BG_FILE_NAME));
    static Path PICTURE_FULL_PATH = Path.of(String.format("%s/%s", IMAGES_FOLDER, PICTURE_FILE_NAME));
    static String CONTENT_TYPE = "Content-Type";
    static String JPEG = "image/jpeg";
    static String PNG = "image/png";
    static String HTML = "text/html, charset=utf-8;";
    static String CSS = "text/css";
    static String LOCALHOST = "localhost";

    public static void main(String[] args) {
        try {
            HttpServer server = makeServer(LOCALHOST, 9889);
            server.start();
            initRoutes(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpServer makeServer(String host, int port) throws IOException {
        InetSocketAddress address = new InetSocketAddress(host, port);
        System.out.printf("Launching server at http://%s:%s/...\n", address.getHostName(), address.getPort());
        HttpServer server = HttpServer.create(address, 50);
        System.out.println("Success!");
        return server;
    }

    private static void initRoutes(HttpServer server) throws IOException {
        server.createContext(String.format("/%s", HTML_FILE_NAME), Main::htmlHandler);
        server.createContext(String.format("/css/%s", CSS_FILE_NAME), Main::cssHandler);
        server.createContext(String.format("/images/%s", PICTURE_FILE_NAME), Main::picHandler);
        server.createContext(String.format("/bg/%s", BG_FILE_NAME), Main::bgHandler);
        server.createContext("/", Main::handleRequest);
        server.createContext("/apps", Main::handleAppsRequest);
        server.createContext("/apps/profile", Main::handleProfileRequest);
    }

    private static void operateTheHeaders(HttpExchange exchange, String mimeType) {
        try {
            exchange.getResponseHeaders().add(CONTENT_TYPE, mimeType);
            int response = 200;
            int length = 0;
            exchange.sendResponseHeaders(response, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeWriter(HttpExchange exchange, Path path) {
        try (Writer writer = getWriter(exchange, path)) {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Writer getWriter(HttpExchange exchange, Path path) throws IOException {
        if (Files.notExists(path)) {
            throw new IOException();
        }
        byte[] byteArray = Files.readAllBytes(path);
        OutputStream output = exchange.getResponseBody();
        output.write(byteArray);
        return new PrintWriter(output, false);
    }

    private static void htmlHandler(HttpExchange exchange) {
        operateTheHeaders(exchange, HTML);
        executeWriter(exchange, HTML_FULL_PATH);
    }

    private static void bgHandler(HttpExchange exchange) {
        operateTheHeaders(exchange, PNG);
        executeWriter(exchange, BG_FULL_PATH);
    }

    private static void picHandler(HttpExchange exchange) {
        operateTheHeaders(exchange, JPEG);
        executeWriter(exchange, PICTURE_FULL_PATH);
    }

    private static void cssHandler(HttpExchange exchange) {
        operateTheHeaders(exchange, CSS);
        executeWriter(exchange, CSS_FULL_PATH);
    }

    private static void handleRequest(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Content-Type", "text/plain, charset=utf-8;");
            int response = 200;
            int length = 0;
            exchange.sendResponseHeaders(response, length);
            try (Writer writer = getWriterFrom(exchange)) {
                String method = exchange.getRequestMethod();
                URI uri = exchange.getRequestURI();
                String ctxPath = exchange.getHttpContext().getPath();
                write(writer, "HTTP Method", method);
                write(writer, "Request", uri.toString());
                write(writer, "Processed via", ctxPath);
                writeHeaders(writer, exchange.getRequestHeaders());
                writeData(writer, exchange);
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void handleProfileRequest(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add(CONTENT_TYPE, HTML);
            exchange.sendResponseHeaders(200, 0);
            try (Writer writer = getWriterFrom(exchange)) {
                String host = exchange.getLocalAddress().getHostName();
                String protocol = exchange.getProtocol();
                int port = exchange.getLocalAddress().getPort();
                write(writer, "Host", host);
                write(writer, "Protocol used", protocol);
                write(writer, "Port", String.valueOf(port));
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void handleAppsRequest(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add(CONTENT_TYPE, "text/cmd, charset=utf-8;");
            exchange.sendResponseHeaders(200, 0);
            try (Writer writer = getWriterFrom(exchange)) {
                String uri = exchange.getRequestURI().toString();
                int hashCode = exchange.hashCode();
                int responseCode = exchange.getResponseCode();
                write(writer, "URI link", uri);
                write(writer, "Hashcode", String.valueOf(hashCode));
                write(writer, "Response Code", String.valueOf(responseCode));
                writeHeaders(writer, exchange.getRequestHeaders());
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Writer getWriterFrom(HttpExchange exchange) {
        OutputStream output = exchange.getResponseBody();
        return new PrintWriter(output, false, StandardCharsets.UTF_8);
    }

    private static void write(Writer writer, String msg, String method) {
        String data = String.format("%s: %s\n\n", msg, method);

        try {
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeHeaders(Writer writer, Headers headers) {
        write(writer, "Request headers", "");
        headers.forEach((k, v) -> write(writer, "\t" + k, v.toString()));
    }

    private static BufferedReader getReader(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
        return new BufferedReader(isr);
    }

    private static void writeData(Writer writer, HttpExchange exchange) {
        try (BufferedReader reader = getReader(exchange)) {
            if (!reader.ready()) {
                return;
            }
            write(writer, "Data Set", "");
            reader.lines().forEach(v -> write(writer, "\t", v));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}