package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrawTKBJava {
    private static final String FILE_COOKIE = "cookies.json";
    private static final Gson gson = new Gson();
    private static final Map<String, Cookie> cookieStore = new ConcurrentHashMap<>();
    private static OkHttpClient client;

    public static void main(String[] args) {
        initializeClient();
        makeRequest();
    }

    private static void initializeClient() {
        if (!loadCookies()) {
            System.err.println("Failed to load cookies: Cookies file does not exist.");
            System.exit(1);
        }

        client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        for (Cookie cookie : cookies) {
                            cookieStore.put(cookie.name(), cookie);
                        }
                        writeCookies();
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return new ArrayList<>(cookieStore.values());
                    }
                })
                .build();
    }

    private static void makeRequest() {
        Request request = new Request.Builder()
                .url("https://daotao.hutech.edu.vn/default.aspx?page=thoikhoabieu&sta=1")
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .addHeader("Accept-Encoding", "gzip, deflate, br, zstd")
                .addHeader("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.8")
                .addHeader("Cache-Control", "max-age=0")
                .addHeader("Connection", "keep-alive")
                .addHeader("Dnt", "1")
                .addHeader("Sec-Ch-Ua-Mobile", "?0")
                .addHeader("Sec-Ch-Ua-Platform", "\"Windows\"")
                .addHeader("Sec-Fetch-Dest", "document")
                .addHeader("Sec-Fetch-Mode", "navigate")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("Sec-Fetch-User", "?1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Request failed with code: " + response.code());
                return;
            }
            String htmlContent = response.body().string();
            writeHtmlContentToFile(htmlContent);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeHtmlContentToFile(String htmlContent) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("output.html"), "UTF-8"))) {
            writer.println(htmlContent);
            processHtmlContent(htmlContent);
        } catch (IOException e) {
            System.err.println("Failed to write HTML content to file: " + e.getMessage());
        }
    }

    private static void processHtmlContent(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent, "UTF-8");

        Elements tds = doc.select("table.body-table td[align='left']");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("TKB.ini"), "UTF-8"))) {
            for (Element td : tds) {
                writer.println(td.text().toString());// xuất ra file tại vì in ra không có dấu và chx biết cách giải quyết
            }
            System.err.println("Done");
        } catch (IOException e) {
            System.err.println("Failed to write processed content to file: " + e.getMessage());
        }
    }

    private static boolean loadCookies() {
        try (FileReader reader = new FileReader(FILE_COOKIE)) {
            JsonObject savedCookies = gson.fromJson(reader, JsonObject.class);
            for (Map.Entry<String, com.google.gson.JsonElement> entry : savedCookies.entrySet()) {
                Cookie cookie = new Cookie.Builder()
                        .name(entry.getKey())
                        .value(entry.getValue().getAsString())
                        .domain("daotao.hutech.edu.vn")
                        .build();
                cookieStore.put(entry.getKey(), cookie);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static void writeCookies() {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, Cookie> entry : cookieStore.entrySet()) {
            jsonObject.addProperty(entry.getKey(), entry.getValue().value());
        }
        try (FileWriter writer = new FileWriter(FILE_COOKIE)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            System.err.println("Failed to write cookies: " + e.getMessage());
        }
    }
}
