import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Client {
    private static final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String url;
    private static String data;
    private static String history;
    private static String cookie;
    private static String noq;
    private static String config;
    private static String token;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Missing config.properties file path");
            System.out.println("Failed");
            return;
        }
        getProperties(config = args[0]);
        readAndSend();
    }

    private static Pojo getPojoObject(String[] values, String startTime, String submitTime) throws IOException {
        Properties prop = new Properties();
        try (final InputStreamReader in = new InputStreamReader(
                new FileInputStream(config), StandardCharsets.UTF_8)) {
            prop.load(in);
            // get the property value and print it out
            List<Answer> answers = new ArrayList<>();
            int count = Integer.parseInt(noq);
            System.out.println("...................................");
            for (int i = 1, index = 0; i <= count; i++) {
                String qId = prop.getProperty("Q" + i);
                String answer = prop.getProperty("A" + i);
                if (answer == null) {
                    answer = values[index++];
                }
                System.out.println("Q[" + i + "] : A[" + i + "] = " + answer);
                answers.add(new Answer(qId, answer.trim()));
            }
            System.out.println("...................................");
            return new Pojo(startTime, submitTime, answers.toString());
        }
    }

    private static void getProperties(String path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            Properties prop = new Properties();
            prop.load(input);
            url = prop.getProperty("URL");
            data = prop.getProperty("READ");
            history = prop.getProperty("WRITE");
            cookie = prop.getProperty("COOKIE");
            noq = prop.getProperty("NOQ");
            token = prop.getProperty("TOKEN");
        }
    }

    private static void writeSentData(String line, String start, String submit, String location)
            throws IOException {
        LocalDateTime now = LocalDateTime.now();
        try (FileWriter writer = new FileWriter(location, true)) {
            writer.write(f.format(now) + "    " + start + "    " + submit + "    " + line + "\n");
        } catch (Exception ex) {
            System.out.println("File write error. Error : " + ex);
        }
    }

    public static void readAndSend() throws RuntimeException {
        Random random = new Random();
        try (BufferedReader br = new BufferedReader(new FileReader(data))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                int delay = 5 + random.nextInt(20);
                System.out.println("\nData will be sent at : " + getNextTime(delay));
                TimeUnit.SECONDS.sleep(delay);
                String[] values = line.split("\t");

                System.out.println("Field count using TAB as separator : " + values.length);

                Pojo pojoObject = getPojoObject(values, getTime(0),
                        getTime(5 + random.nextInt(50)));

                ObjectMapper mapper = new ObjectMapper();

                String jsonData = mapper.writeValueAsString(pojoObject);

                System.out.println("Sending data : " + Arrays.toString(values));
                if (postData(jsonData, url)) {
                    writeSentData(line, pojoObject.startDate, pojoObject.submitDate, history);
                } else {
                    System.out.println("\nFailed.");
                    return;
                }
            }
            System.out.println("\nDone");
        } catch (IOException | InterruptedException e) {
            System.out.println("\nError : " + e);
        }
    }

    private static String getNextTime(int seconds) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        calendar.add(Calendar.SECOND, seconds);
        return format.format(calendar.getTime());
    }

    private static String getTime(int seconds) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        calendar.add(Calendar.SECOND, seconds);
        return format.format(calendar.getTime());
    }

    public static boolean postData(String jsonData, String URL) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(URL);
            StringEntity params = new StringEntity(jsonData);
            System.out.println("JSON Data : \n" + jsonData);
            post.addHeader("content-type", "application/json");
            post.addHeader("Accept-Language", "en-US,en;q=0.5");
            post.addHeader("Accept-Encoding", "gzip, deflate, br, zstd");
            post.addHeader("__requestverificationtoken", token);
            post.addHeader("Cookie", readCookies(cookie));
            post.setEntity(params);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.out.println("\nStatus : " + response.getCode() + "[" + response.getReasonPhrase() + "]");
                if (response.getCode() != 201) {
                    System.out.println("Response : \n" + responseBody);
                }
                System.out.println("_________________________________________________");
                return response.getCode() == 201;
            }
        } catch (Throwable ex) {
            System.out.println("\nStatus : Failed (" + ex + ")");
            ex.printStackTrace();
            System.out.println("_________________________________________________");
            return false;
        }
    }

    private static String readCookies(String path) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        StringBuilder cookiesStr = new StringBuilder();
        Object obj = parser.parse(new FileReader(path));

        JSONObject jsonObject = (JSONObject) obj;
        JSONObject requestCookies = (JSONObject) jsonObject.get("Request Cookies");

        cookiesStr.append("__RequestVerificationToken=");
        cookiesStr.append(requestCookies.get("__RequestVerificationToken"));
        cookiesStr.append(";");

        cookiesStr.append("AADAuth.forms=");
        cookiesStr.append(requestCookies.get("AADAuth.forms"));
        cookiesStr.append(";");

        cookiesStr.append("AADAuthCode.forms=");
        cookiesStr.append(requestCookies.get("AADAuthCode.forms"));
        cookiesStr.append(";");

        cookiesStr.append("FormsWebSessionId=");
        cookiesStr.append(requestCookies.get("FormsWebSessionId"));
        cookiesStr.append(";");

        cookiesStr.append("MicrosoftApplicationsTelemetryDeviceId=");
        cookiesStr.append(requestCookies.get("MicrosoftApplicationsTelemetryDeviceId"));
        cookiesStr.append(";");

        cookiesStr.append("MUID=");
        cookiesStr.append(requestCookies.get("MUID"));
        cookiesStr.append(";");

        cookiesStr.append("OIDCAuth.forms=");
        cookiesStr.append(requestCookies.get("OIDCAuth.forms"));

        return cookiesStr.toString();
    }
}

