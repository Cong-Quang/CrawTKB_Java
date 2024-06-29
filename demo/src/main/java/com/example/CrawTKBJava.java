package com.example;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp chính của chương trình.
 *
 * @author <tên của bạn>
 */
public class CrawTKBJava {
    /** Đường dẫn đến tệp cookies.json */
    private static final String FILE_COOKIE = "cookies.json";
    /** Đối tượng Gson để đọc/ghi định dạng JSON */
    private static final Gson gson = new Gson();
    /** Bộ nhớ cookie để lưu trữ các cookies được lưu trữ */
    private static final Map<String, Cookie> cookieStore = new ConcurrentHashMap<>();
    /** Đối tượng OkHttpClient để thực hiện các yêu cầu HTTP */
    private static OkHttpClient client;

    /**
     * Hàm chính của chương trình.
     *
     * @param args các đối số dòng lệnh
     */
    public static void main(String[] args) {
        // Tải cookies từ tệp cookies.json
        if (!loadCookies()) {
            System.err.println("Không thể tải cookies: Tệp cookies.json không tồn tại.");
            System.exit(1);
        }

        // Cấu hình OkHttpClient với cookie jar
        client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    /**
                     * Lưu cookies nhận được từ máy chủ vào bộ nhớ cookie.
                     *
                     * @param url URL từ đó cookies được nhận
                     * @param cookies danh sách cookies cần lưu
                     */
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        for (Cookie cookie : cookies) {
                            cookieStore.put(cookie.name(), cookie);
                        }
                        writeCookies();
                    }

                    /**
                     * Trả về danh sách cookies cần gửi đến máy chủ từ bộ nhớ cookie.
                     *
                     * @param url URL cho mà cookies được cần
                     * @return danh sách cookies cần gửi
                     */
                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return new ArrayList<>(cookieStore.values());
                    }
                })
                .build();

        // Tạo một yêu cầu đến trang https://daotao.hutech.edu.vn/default.aspx?page=thoikhoabieu&sta=1
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
            // Kiểm tra xem yêu cầu có thành công hay không
            if (!response.isSuccessful()) {
                System.err.println("Yêu cầu thất bại với mã: " + response.code());
                return;
            }

            // Viết body của phản hồi vào tệp output.html với bảng mã Unicode
            String htmlContent;
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("output.html"), "UTF-8"))) {
                htmlContent = response.body().string();
                
                writer.println(htmlContent);
            }
            processHtmlContent(htmlContent);
            // Xử lý nội dung HTML
           

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Đọc cookies từ tệp cookies.json và lưu trữ vào bộ nhớ cookie.
     *
     * @return {@code true} nếu đọc cookies thành công, {@code false} nếu tệp cookies.json không tồn tại
     */
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

    /**
     * Ghi cookies từ bộ nhớ cookie vào tệp cookies.json.
     */
    private static void writeCookies() {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, Cookie> entry : cookieStore.entrySet()) {
            jsonObject.addProperty(entry.getKey(), entry.getValue().value());
        }
        try (FileWriter writer = new FileWriter(FILE_COOKIE)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            System.err.println("Không thể ghi cookies: " + e.getMessage());
        }
    }

    /**
     * Xử lý nội dung HTML để trích xuất thông tin từ các thẻ <td>.
     *
     * @param htmlContent nội dung HTML cần xử lý
     */
    private static void processHtmlContent(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent, "UTF-8");

        Elements tds = doc.select("table.body-table td[align='left']");

        for (Element td : tds) {
            Element div = td.selectFirst("div");
            if (div != null) {
                System.out.println("Div content: " + div.text());
            } else {
                System.out.println("TD content: " + td.text());
            }
        }
    }
}
