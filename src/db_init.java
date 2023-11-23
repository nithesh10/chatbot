import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class db_init {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chats";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            createChatsTable(connection);
            System.out.println("Table 'chats' created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createChatsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // SQL query to create the 'chats' table
            String query = "CREATE TABLE chats (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_message VARCHAR(255)," +
                    "chatbot_response VARCHAR(255)," +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            
            // Execute the query
            statement.executeUpdate(query);
        }
    }
}
