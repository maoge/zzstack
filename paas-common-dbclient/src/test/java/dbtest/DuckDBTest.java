package dbtest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DuckDBTest {

    public static void main(String[] args) {
        Properties dbProperty = new Properties();
        dbProperty.setProperty("duckdb.read_only", "false");
        dbProperty.setProperty("temp_directory", "./data/");
        
        try {
            Connection conn = DriverManager.getConnection("jdbc:duckdb:/data/my_database", dbProperty);
            
            // create a table
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE items (item VARCHAR, value DECIMAL(10, 2), count INTEGER)");
            // insert two items into the table
            stmt.execute("INSERT INTO items VALUES ('jeans', 20.0, 1), ('hammer', 42.2, 2)");
    
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM items")) {
                while (rs.next()) {
                    System.out.println(rs.getString(1));
                    System.out.println(rs.getInt(3));
                }
            }
            stmt.close();
        
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
