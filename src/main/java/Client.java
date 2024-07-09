import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Client {
    private static final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        readAndSend(getProperties(args[0]), args[0]);
    }

    private static Pojo getPojoObject(String[] values, String startTime, String submitTime,
                                      String path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            Properties prop = new Properties();
            prop.load(input);
            // get the property value and print it out
            List<Answer> answers = new ArrayList<>();
            int noq = Integer.parseInt(prop.getProperty("NOQ"));
            for (int i = 1, index = 0; i <= noq; i++) {
                String qId = prop.getProperty("Q" + i);
                String answer = prop.getProperty("A" + i);
                if (answer == null) {
                    answer = values[index++];
                }
                System.out.println("Q[" + i + "] : A[" + i + "] = " + answer);
                answers.add(new Answer(qId, answer));
            }
            return new Pojo(startTime, submitTime, answers.toString());
        }
    }

    private static String[] getProperties(String path) throws IOException {
        List<String> propList = new ArrayList<>();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            Properties prop = new Properties();
            prop.load(input);
            // get the property value and print it out
            propList.add(prop.getProperty("URL"));
            propList.add(prop.getProperty("READ"));
            propList.add(prop.getProperty("WRITE"));
            propList.add(prop.getProperty("DATA"));
        }
        return propList.toArray(new String[0]);
    }

    private static void writeSentData(String line, String start, String submit, String location)
            throws IOException {
        LocalDateTime now = LocalDateTime.now();
        try (FileWriter writer = new FileWriter(location, true)) {
            writer.write(f.format(now) + "    " + start + "    " + submit + "    " + line + "\n");
        } catch (Exception ex) {
            System.out.println("FILE WRITE ERROR. ERROR : " + ex);
            ex.printStackTrace();
        }
    }

    public static void readAndSend(String[] props, String path) throws RuntimeException {
        Random random = new Random();
        try (BufferedReader br = new BufferedReader(new FileReader(props[1]))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                int delay = 5 + random.nextInt(20);
                System.out.println("\nNEXT DATA SEND TIME " + getNextTime(delay));
                TimeUnit.SECONDS.sleep(delay);
                String[] values = line.split("\t");

                System.out.println("Field count using TAB as separator : " + values.length);

                Pojo pojoObject = getPojoObject(values, getTime(0),
                        getTime(5 + random.nextInt(50)), path);

                ObjectMapper mapper = new ObjectMapper();

                String jsonData = mapper.writeValueAsString(pojoObject);

                System.out.println("SENDING DATA : " + Arrays.toString(values));
                if (postData(jsonData, props[0])) {
                    writeSentData(line, pojoObject.startDate, pojoObject.submitDate, props[2]);
                } else {
                    System.out.println("\nEXITING.");
                    return;
                }
            }
            System.out.println("\nDONE");
        } catch (IOException | InterruptedException e) {
            System.out.println("\nERROR : " + e);
            e.printStackTrace();
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
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPost request = new HttpPost(URL);
            StringEntity params = new StringEntity(jsonData);
            System.out.println("JSON Data : \n" + jsonData);
            request.addHeader("content-type", "application/json");
            request.addHeader("Accept-Language", "en-US,en;q=0.5");
            request.addHeader("Accept-Encoding", "gzip, deflate, br, zstd");
            request.addHeader("x-ms-form-request-source", "ms-formweb");
            request.addHeader("x-ms-form-request-ring", "business");
            request.addHeader("odata-maxverion", "4.0");
            request.addHeader("odata-version", "4.0");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            if (response.getCode() == 201) {
                System.out.println("\nSTATUS : SUCCESS (201)");
                System.out.println("_________________________________________________");
                return true;
            } else {
                System.out.println("\nSTATUS : FAILED (" + response.getCode() + ")");
                System.out.println("_________________________________________________");
            }
        } catch (Throwable ex) {
            System.out.println("\nSTATUS : FAILED (" + ex + ")");
            System.out.println("_________________________________________________");
            ex.printStackTrace();
            return false;
        }
        return false;
    }
}
