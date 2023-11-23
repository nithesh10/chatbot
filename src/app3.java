import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class app3 extends Application {

    private TextArea chatArea;
    private TextField userInputField;
    private ListView<String> chatTitlesListView;
    private Map<String, ObservableList<String>> chatHistoryMap;

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chats";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // JDBC variables for opening, closing, and managing connection
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

        // Sidebar with chat titles
        chatTitlesListView = new ListView<>();
        chatTitlesListView.setOnMouseClicked(event -> onChatTitleClicked());

        // Map to store chat history for each chat title
        chatHistoryMap = new HashMap<>();

        VBox vbox = new VBox(chatArea, userInputField, sendButton);
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(createMenuBar());
        borderPane.setCenter(vbox);
        borderPane.setLeft(chatTitlesListView);

        Scene scene = new Scene(borderPane, 600, 400);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem newChatMenuItem = new MenuItem("New Chat");
        newChatMenuItem.setOnAction(e -> openNewChat());
        MenuItem historyMenuItem = new MenuItem("Chat History");
        historyMenuItem.setOnAction(e -> displayChatHistory());
        fileMenu.getItems().addAll(newChatMenuItem, historyMenuItem);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private void openNewChat() {
        TextInputDialog dialog = new TextInputDialog("Chat Title");
        dialog.setTitle("New Chat");
        dialog.setHeaderText("Enter a title for the new chat:");
        dialog.setContentText("Title:");

        String chatTitle = dialog.showAndWait().orElse(null);

        if (chatTitle != null && !chatTitle.isEmpty()) {
            // Add the new chat title to the sidebar
            chatTitlesListView.getItems().add(chatTitle);
            // Initialize an empty chat history for the new chat title
            chatHistoryMap.put(chatTitle, FXCollections.observableArrayList());
            // Create a table for the new chat title in the database
            createChatTable(chatTitle);
        }
    }

    private void createChatTable(String chatTitle) {
        try (Statement statement = connection.createStatement()) {
            String query = "CREATE TABLE IF NOT EXISTS " + chatTitle + " ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "user_message VARCHAR(255),"
                    + "chatbot_response VARCHAR(255),"
                    + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

            // Insert the chat into the database and update the chat history
            String currentChatTitle = chatTitlesListView.getSelectionModel().getSelectedItem();
            if (currentChatTitle != null) {
                insertChatIntoDatabase(currentChatTitle, userMessage, chatbotResponse);
                updateChatHistory(currentChatTitle, "You: " + userMessage, "Chatbot: " + chatbotResponse);
            }
        }

        userInputField.clear();
    }

    private void insertChatIntoDatabase(String chatTitle, String userMessage, String chatbotResponse) {
        try {
            // Create a prepared statement
            String query = "INSERT INTO " + chatTitle + " (user_message, chatbot_response) VALUES (?, ?)";
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

    private void updateChatHistory(String chatTitle, String... messages) {
        ObservableList<String> chatHistory = chatHistoryMap.get(chatTitle);
        if (chatHistory != null) {
            chatHistory.addAll(messages);
        }
    }

    private void displayChatHistory() {
        String selectedChatTitle = chatTitlesListView.getSelectionModel().getSelectedItem();
        if (selectedChatTitle != null) {
            ObservableList<String> chatHistory = chatHistoryMap.get(selectedChatTitle);
            if (chatHistory != null) {
                Stage historyStage = new Stage();
                VBox historyVBox = new VBox();
                for (String message : chatHistory) {
                    historyVBox.getChildren().add(new Label(message));
                }

                Scene historyScene = new Scene(historyVBox, 300, 300);
                historyStage.setScene(historyScene);
                historyStage.setTitle("Chat History - " + selectedChatTitle);
                historyStage.show();
            }
        }
    }

    private void onChatTitleClicked() {
        // Handle the event when a chat title is clicked in the sidebar
        String selectedChatTitle = chatTitlesListView.getSelectionModel().getSelectedItem();
        if (selectedChatTitle != null) {
            // Retrieve chat history for the selected chat title and display it
            displayChatHistoryForChatTitle(selectedChatTitle);
        }
    }

    private void displayChatHistoryForChatTitle(String chatTitle) {
        ObservableList<String> chatHistory = chatHistoryMap.get(chatTitle);
        if (chatHistory != null) {
            Stage historyStage = new Stage();
            VBox historyVBox = new VBox();
            for (String message : chatHistory) {
                historyVBox.getChildren().add(new Label(message));
            }

            Scene historyScene = new Scene(historyVBox, 300, 300);
            historyStage.setScene(historyScene);
            historyStage.setTitle("Chat History - " + chatTitle);
            historyStage.show();
        }
    }

    private void appendToChatArea(String message) {
        chatArea.appendText(message + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
