import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class App extends Application {

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
        Dialog<Pair<String, String>> loginDialog = createLoginDialog();

        // Show the login dialog and wait for user input
        Optional<Pair<String, String>> result = loginDialog.showAndWait();

        // Check if the login was successful
        if (result.isPresent() && isValidCredentials(result.get())) {
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
            loadChatTitlesAndHistories();

            // Initially open a new chat
            //openNewChat();
        }
        else {
            // Close the application if login fails
            primaryStage.close();
        }
    }
    private Dialog<Pair<String, String>> createLoginDialog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login Dialog");
        dialog.setHeaderText("Please enter your credentials:");

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the login form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button based on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        return dialog;
    }

    private boolean isValidCredentials(Pair<String, String> credentials) {
        // Check if the entered username and password are valid
        return "nithesh".equals(credentials.getKey()) && "password".equals(credentials.getValue());
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

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(chatTitle -> {
            // Add the new chat title to the sidebar
            chatTitlesListView.getItems().add(chatTitle);
            // Initialize an empty chat history for the new chat title
            chatHistoryMap.put(chatTitle, FXCollections.observableArrayList());
            // Create a table for the new chat title in the database
            createChatTable(chatTitle);
        });
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
    private void loadChatTitlesAndHistories() {
        try {
            // Get all table names from the database
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables("chats", null, null, new String[]{"TABLE"});
    
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                chatTitlesListView.getItems().add(tableName);
    
                // Load chat history for each table
                ObservableList<String> chatHistory = loadChatHistory(tableName);
                chatHistoryMap.put(tableName, chatHistory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private ObservableList<String> loadChatHistory(String tableName) {
        ObservableList<String> chatHistory = FXCollections.observableArrayList();
        try (Statement statement = connection.createStatement()) {
            String query = "SELECT user_message, chatbot_response FROM " + tableName;
            ResultSet resultSet = statement.executeQuery(query);
    
            while (resultSet.next()) {
                String userMessage = resultSet.getString("user_message");
                String chatbotResponse = resultSet.getString("chatbot_response");
                chatHistory.add("User: " + userMessage);
                chatHistory.add("Chatbot: " + chatbotResponse);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatHistory;
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
            chatArea.clear();

        for (String message : chatHistory) {
            chatArea.appendText(message + "\n");
        }
        }
    }

    private void appendToChatArea(String message) {
        chatArea.appendText(message + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
