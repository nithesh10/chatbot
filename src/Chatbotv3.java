import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Chatbotv3 {

    private static final String OPENAI_API_KEY = "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";
    private static final String OPENAI_API_ENDPOINT = "https://api.openai.com/v1/chat/completions"; // Updated endpoint

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("You: ");
            String userMessage = scanner.nextLine();

            if (userMessage.equalsIgnoreCase("exit")) {
                System.out.println("Chatbot: Goodbye!");
                break;
            }

            String chatbotResponse = getChatbotResponse(userMessage);
            System.out.println("Chatbot: " + chatbotResponse);
        }
    }

    static String getChatbotResponse(String userMessage) {
        try {
            URL url = new URL(OPENAI_API_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            connection.setDoOutput(true);

            // Create the request JSON payload
            String jsonPayload = String.format(
                    "{ \"model\": \"gpt-3.5-turbo\", \"messages\": [ { \"role\": \"system\", \"content\": \"You are a helpful assistant.\" }, { \"role\": \"user\", \"content\": \"%s\" } ] }",
                    userMessage);

            try (DataOutputStream os = new DataOutputStream(connection.getOutputStream())) {
                os.writeBytes(jsonPayload);
                os.flush();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    
                    
                    // Extract the chatbot response text
                    return extractChatbotResponseText(response.toString());
                }
            } else {
                return "Error: " + responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred.";
        }
    }

    private static String extractChatbotResponseText(String response) {

        int startIndex = response.indexOf("\"content\": \"") + 12; // Update the index to include the colon and space
        int endIndex = response.indexOf("\"", startIndex);
        return response.substring(startIndex, endIndex);
    }
    
}
