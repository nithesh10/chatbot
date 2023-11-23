import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
public class App extends Application {
    private TextArea chatArea;
    private TextField userInputField;
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // JDBC variables for opening, closing and managing connection
    private static Connection connection;

    static {
        try {
            // Initialize the connection
            connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
            System.out.println("database connected"+connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        // Close the connection when the application is closed
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chatbot");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        userInputField = new TextField();
        Button sendButton = new Button("Send");

        // Apply CSS styling
        chatArea.getStyleClass().add("chat-area");
        userInputField.getStyleClass().add("user-input");
        sendButton.getStyleClass().add("send-button");

        sendButton.setOnAction(e -> onSendButtonClicked());

        VBox vbox = new VBox(chatArea, userInputField, sendButton);
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));

        // Apply CSS styling to the VBox
        vbox.getStyleClass().add("main-vbox");

        Scene scene = new Scene(vbox, 400, 400);

        // Apply a CSS stylesheet to the scene
        scene.getStylesheets().add("styles.css");

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void onSendButtonClicked() {
        String userMessage = userInputField.getText();
        appendToChatArea("You: " + userMessage);

        if (userMessage.equalsIgnoreCase("exit")) {
            appendToChatArea("Chatbot: Goodbye!");
            userInputField.setDisable(true);
        } else {
            String chatbotResponse = Chatbotv3.getChatbotResponse(userMessage);
            appendToChatArea("Chatbot: " + chatbotResponse);
        }

        userInputField.clear();
    }

    private void appendToChatArea(String message) {
        chatArea.appendText(message + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
