import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class app2 extends Application {

    private TextArea chatArea;
    private TextField userInputField;

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chats";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // JDBC variables for opening, closing and managing connection
    private static Connection connection;

    static {
        try {
            // Initialize the connection
            connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
            System.out.println("database connected" + connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
    try {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Database connection closed.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
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

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem historyMenuItem = new MenuItem("Chat History");
        historyMenuItem.setOnAction(e -> displayChatHistory());
        fileMenu.getItems().add(historyMenuItem);
        menuBar.getMenus().add(fileMenu);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(menuBar);
        borderPane.setCenter(vbox);

        Scene scene = new Scene(borderPane, 400, 400);

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

            // Insert the chat into the database
            insertChatIntoDatabase(userMessage, chatbotResponse);
        }

        userInputField.clear();
    }

    private void insertChatIntoDatabase(String userMessage, String chatbotResponse) {
        try {
            // Create a prepared statement
            String query = "INSERT INTO chats (user_message, chatbot_response) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, userMessage);
                preparedStatement.setString(2, chatbotResponse);

                // Execute the update
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayChatHistory() {
        try {
            // Create a statement
            Statement statement = connection.createStatement();

            // Execute a query to fetch chat history
            String query = "SELECT user_message, chatbot_response FROM chats";
            ResultSet resultSet = statement.executeQuery(query);

            // Display chat history in a new window or dialog
            Stage historyStage = new Stage();
            VBox historyVBox = new VBox();
            while (resultSet.next()) {
                String userMessage = resultSet.getString("user_message");
                String chatbotResponse = resultSet.getString("chatbot_response");
                historyVBox.getChildren().add(new Label("User: " + userMessage));
                historyVBox.getChildren().add(new Label("Chatbot: " + chatbotResponse));
            }

            Scene historyScene = new Scene(historyVBox, 300, 300);
            historyStage.setScene(historyScene);
            historyStage.setTitle("Chat History");
            historyStage.show();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void appendToChatArea(String message) {
        chatArea.appendText(message + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
