import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        readAndSend(getProperties(args[0]), args[0]);
    }

    private static Pojo getPojoObject(String[] values, String startTime, String submitTime,
                                      String path) {
        try (InputStream input = new BufferedInputStream(new FileInputStream(path))) {
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
                answers.add(new Answer(qId, answer));
            }
            return new Pojo(startTime, submitTime, answers.toString());
        } catch (IOException ex) {
            System.out.println("Error occurred while reading config properties. Error : " + ex);
            return null;
        }
    }

    private static String[] getProperties(String path) {
        List<String> props = new ArrayList<>();
        try (InputStream input = new BufferedInputStream(new FileInputStream(path))) {
            Properties prop = new Properties();
            prop.load(input);
            // get the property value and print it out
            props.add(prop.getProperty("URL"));
            props.add(prop.getProperty("READ"));
            props.add(prop.getProperty("WRITE"));
            props.add(prop.getProperty("DATA"));
        } catch (IOException ex) {
            System.out.println("Error occurred while reading config properties. Error : " + ex);
        }
        return props.toArray(new String[0]);
    }

    private static void writeSentData(String line, String location) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        try (FileWriter writer = new FileWriter(location, true)) {
            writer.write(f.format(now) + "    " + line + "\n");
        } catch (Exception ex) {
            System.out.println("Error occurred while writing to file. Error : " + ex);
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
                int delay = random.nextInt(25);
                System.out.println("---------------------   NEXT DATA SEND TIME " + getNextTime(delay));
                TimeUnit.SECONDS.sleep(delay);
                String[] values = line.split("\t");

                Pojo pojoObject = getPojoObject(values, getTime(0), getTime(random.nextInt(50)),
                        path);

                ObjectMapper mapper = new ObjectMapper();

                String jsonData = mapper.writeValueAsString(pojoObject);

                System.out.println("---------------------   SENDING DATA : " + Arrays.toString(values));
                if (postData(jsonData, props[0])) {
                    writeSentData(line, props[2]);
                } else {
                    System.out.println("---------------------   FAILED. EXITING.");
                    return;
                }
            }
            System.out.println("---------------------   DONE");
        } catch (IOException | InterruptedException e) {
            System.out.println("Error : " + e);
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
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            if (response.getCode() == 201) {
                System.out.println("---------------------   STATUS : SUCCESS");
                return true;
            }
        } catch (Throwable ex) {
            System.out.println("---------------------   STATUS : FAILED");
            System.out.println("---------------------   ERROR : " + ex);
            return false;
        }
        return false;
    }
}
