package com.raven.service;

import com.raven.connection.DatabaseConnection;
import com.raven.model.Model_Client;
import com.raven.model.Model_Login;
import com.raven.model.Model_Message;
import com.raven.model.Model_Register;
import com.raven.model.Model_User_Account;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

public class ServiceUser {

    private static final Logger LOGGER = Logger.getLogger(ServiceUser.class.getName());

    public ServiceUser() {
        this.con = DatabaseConnection.getInstance().getConnection();
    }

    public Model_Message register(Model_Register data) {
        Model_Message message = new Model_Message();
        try (PreparedStatement checkStatement = con.prepareStatement(CHECK_USER)) {
            checkStatement.setString(1, data.getUserName());
            LOGGER.log(Level.INFO, "Checking if user exists: " + data.getUserName());
            try (ResultSet r = checkStatement.executeQuery()) {
                if (r.first()) {
                    LOGGER.log(Level.WARNING, "User already exists: " + data.getUserName());
                    message.setAction(false);
                    message.setMessage("User Already Exists");
                } else {
                    LOGGER.log(Level.INFO, "User does not exist, continue to register: " + data.getUserName());
                    message.setAction(true);
                }
            }
             if (message.isAction()) {
                 con.setAutoCommit(false);
                 try (PreparedStatement insertStatement = con.prepareStatement(INSERT_USER, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertStatement.setString(1, data.getUserName());
                    insertStatement.setString(2, data.getPassword());
                    LOGGER.log(Level.INFO, "Inserting new user: " + data.getUserName());
                    insertStatement.execute();
                    try(ResultSet generatedKeys = insertStatement.getGeneratedKeys()){
                        if (generatedKeys.next()) {
                            int userID = generatedKeys.getInt(1);
                            try (PreparedStatement accountStatement = con.prepareStatement(INSERT_USER_ACCOUNT)) {
                                accountStatement.setInt(1, userID);
                                accountStatement.setString(2, data.getUserName());
                                LOGGER.log(Level.INFO, "Creating user account for user ID: " + userID);
                                accountStatement.execute();
                                con.commit();
                                 LOGGER.log(Level.INFO, "Registration successful for user: " + data.getUserName());
                                message.setAction(true);
                                message.setMessage("Registration Successful");
                                JSONObject jsonObject = new JSONObject(new Model_User_Account(userID, data.getUserName(), "", "", true));
                                message.setData(data);
                            }
                        }
                        else {
                             LOGGER.log(Level.WARNING, "Failed to retrieve generated keys for user: " + data.getUserName());
                            message.setAction(false);
                            message.setMessage("Failed to retrieve generated keys");
                            con.rollback();
                        }
                    }
                }
             }
        } catch (SQLException e) {
            message.setAction(false);
            message.setMessage("Server Error");
            LOGGER.log(Level.SEVERE, "SQL Exception during registration:", e);
            try {
                if (!con.getAutoCommit()) {
                    con.rollback();
                   }
            } catch (SQLException rollbackException) {
               LOGGER.log(Level.SEVERE, "SQL Exception during rollback:", rollbackException);
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException setAutoCommitException) {
                    LOGGER.log(Level.SEVERE, "SQL Exception during setting autocommit:", setAutoCommitException);
                }
            }

        }
        return message;
    }

    public Model_User_Account login(Model_Login login) throws SQLException {
        LOGGER.log(Level.INFO, "Attempting login for user: " + login.getUserName());
        Model_User_Account data = null;
        try (PreparedStatement p = con.prepareStatement(LOGIN)) {
            p.setString(1, login.getUserName());
            p.setString(2, login.getPassword());
            LOGGER.log(Level.INFO, "Executing login query for user: " + login.getUserName());
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    int userID = r.getInt(1);
                    String userName = r.getString(2);
                    String gender = r.getString(3);
                    String image = r.getString(4);
                    data = new Model_User_Account(userID, userName, gender != null ? gender : "", image != null ? image : "", true);
                    LOGGER.log(Level.INFO, "Login successful for user: " + login.getUserName());
                } else {
                    LOGGER.log(Level.WARNING, "Login failed for user: " + login.getUserName() + " , no user found.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Exception during login:", e);
            throw e;
        }
        return data;
    }

    public List<Model_User_Account> getUser(int exitUser) throws SQLException {
        List<Model_User_Account> list = new ArrayList<>();
        try (PreparedStatement p = con.prepareStatement(SELECT_USER_ACCOUNT)) {
            p.setInt(1, exitUser);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int userID = r.getInt(1);
                    String userName = r.getString(2);
                    String gender = r.getString(3);
                    String image = r.getString(4);
                    list.add(new Model_User_Account(userID, userName, gender != null ? gender : "", image != null ? image : "", checkUserStatus(userID)));
                }
            }
        }
        return list;
    }

    private boolean checkUserStatus(int userID) {
        List<Model_Client> clients = Service.getInstance(null).getListClient();
        for (Model_Client c : clients) {
            if (c.getUser().getUserID() == userID) {
                return true;
            }
        }
        return false;
    }

    //  SQL
    private final String LOGIN = "select UserID, user_account.UserName, Gender, ImageString from user join user_account using (UserID) where user.UserName=BINARY(?) and user.Password=BINARY(?) and user_account.Status='1'";
    private final String SELECT_USER_ACCOUNT = "select UserID, UserName, Gender, ImageString from user_account where user_account.Status='1' and UserID<>?";
    private final String INSERT_USER = "insert into user (UserName, Password) values (?,?)";
    private final String INSERT_USER_ACCOUNT = "insert into user_account (UserID, UserName) values (?,?)";
    private final String CHECK_USER = "select UserID from user where UserName =? limit 1";
    //  Instance
    private final Connection con;
}