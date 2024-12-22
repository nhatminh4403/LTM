package com.raven.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static DatabaseConnection instance;
    private Connection connection;

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private DatabaseConnection() {
    try {
            connectToDatabase();
        } catch(SQLException e) {
             LOGGER.log(Level.SEVERE, "SQL Exception during database connection:", e);
        }

    }

   public void connectToDatabase() throws SQLException{
        String server = "localhost";
        String port = "3306";
        String database = "chat";
        String userName = "root";
        String password = "";
        connection = java.sql.DriverManager.getConnection("jdbc:mysql://" + server + ":" + port + "/" + database, userName, password);
      if(connection != null){
          System.out.println("Connection to database established successfully.");
      } else {
         System.out.println("Connection to database failed.");
       }

    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
