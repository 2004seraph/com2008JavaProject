package entity.user;

import db.DatabaseBridge;
import db.DatabaseOperation;
import db.DatabaseRecord;
import entity.Address;
import entity.BankDetail;
import entity.order.Order;

import java.security.InvalidKeyException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Person extends DatabaseOperation.Entity implements DatabaseRecord {
    public enum Role {
        // Number represents where you are in the hierarchy
        USER(0), STAFF(1), MANAGER(2);

        private final int priviledgeLevel;
        private Role(int value) {
            this.priviledgeLevel = value;
        }

        public int getLevel() {
            return priviledgeLevel;
        }

        public static String[] getStringValues() {
            return Arrays.stream(values()).map(Enum::toString).toArray(String[]::new);
        }
    }

    public static class PersonNotFoundException extends RuntimeException {
        public PersonNotFoundException(String msg) {
            super(msg);
        }
    }

    // -1 means the Person exists only in Java and hasn't had an entry made in the db for it
    private int personID = -1;
    private String forename;
    private String surname;
    private String email;
    private String password;
    private String houseNumber;
    private String postCode;
    private int bankDetailsID = -1;

    private Address address;
    private BankDetail bankDetail;

    private Role role = Role.USER;

    public Address getAddress() {
        return address;
    }
    public BankDetail getBankDetail() {
        return bankDetail;
    }

    public int getId() { return personID; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getForename() { return forename; }
    public String getSurname() { return surname; }
    public String getFullName() { return forename + " " + surname; }
    public int getBankDetailsId() { return bankDetailsID; }
    public String getHouseNumber() { return houseNumber; }
    public String getPostCode() { return postCode; }

    /**
     * This blank constructor is for creating a brand-new Person (not in the database yet)
     */
    private Person() {

    }

    /**
     * Another constructor to use when creating a brand-new person,
     * note that the id is not included (generated when inserting)
     * @param forename
     * @param surname
     * @param email
     * @param password
     * @param houseNumber
     * @param postCode
     */
    public Person(
            String forename,
            String surname,
            String email,
            String password,
            String houseNumber,
            String postCode
    ) {
        this.forename =      forename;
        this.surname =       surname;
        this.email =         email;
        this.password =      password;
        this.houseNumber =     houseNumber;
        this.postCode =      postCode;
    }

    /**
     * The constructor to use when retrieving a pre-existing person from the database,
     * Note the inclusion of the id
     * @param id
     * @param forename
     * @param surname
     * @param email
     * @param password
     * @param houseNumber
     * @param postCode
     * @param bankDetailsID
     */
    private Person(
            int id,
            String forename,
            String surname,
            String email,
            String password,
            String houseNumber,
            String postCode,
            int bankDetailsID,
            Role role,
            boolean decrypt
    ) throws SQLException, InvalidKeyException {
        this.personID =      id;
        this.forename =      forename;
        this.surname =       surname;
        this.email =         email;
        this.password =      password;
        this.houseNumber =     houseNumber;
        this.postCode =      postCode;
        this.bankDetailsID = bankDetailsID;
        this.role = role;

        reloadAddress();

        if (decrypt) {
            try {
                BankDetail bankDetail = BankDetail.getBankDetailsById(bankDetailsID);
                this.bankDetail = bankDetail;
            } catch (BankDetail.BankAccountNotFoundException e) {
                this.bankDetail = null;
            }
        }
    }

    public void reloadAddress() throws SQLException {
        Address address = Address.getAddressById(houseNumber, postCode);
        if (address == null) {
            throw new SQLException("No address found with that house number and postcode");
        }
        this.address = address;
    }

    /**
     * Returns a result set of every person in the database
     * @return A resultSet
     */
    public static ResultSet getAllPersons() {
        try {
            openConnection();
            PreparedStatement query = prepareStatement("SELECT * FROM Person");
            return query.executeQuery();
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to fetch all persons", e);
            throw new RuntimeException();
        } finally {
            closeConnection();
        }
    }

    /**
     * Gets a person from the database using their email
     * @param email A string of an email, can be valid or invalid
     * @return A Person object with all of its fields set, or null if there was no one with that email
     * @throws SQLException
     */
    public static Person getPersonByEmail(String email) throws SQLException {
        PreparedStatement personQuery = prepareStatement("SELECT * FROM Person WHERE email=?");
        personQuery.setString(1, email);
        ResultSet res = personQuery.executeQuery();
        return personFromResultSet(res, true);
    }
    public static Person getPersonByEmail(String email, boolean status) throws SQLException {
        PreparedStatement personQuery = prepareStatement("SELECT * FROM Person WHERE email=?");
        personQuery.setString(1, email);
        ResultSet res = personQuery.executeQuery();
        return personFromResultSet(res, status);
    }

    /**
     * Gets a person from the database using their email, DOES NOT SET THE BANK DETAILS FIELDS
     * @param id The id of a person
     * @return A Person object with all of its fields set, or null if there was no one with that id
     * @throws SQLException
     */
    public static Person getPersonByID(int id) throws SQLException {
        PreparedStatement personQuery = prepareStatement("SELECT * FROM Person WHERE personId=?");
        personQuery.setInt(1, id);
        ResultSet res = personQuery.executeQuery();
        return personFromResultSet(res, false);
    }

    public static Person getPersonByID(int id, boolean decrypt) throws SQLException {
        PreparedStatement personQuery = prepareStatement("SELECT * FROM Person WHERE personId=?");
        personQuery.setInt(1, id);
        ResultSet res = personQuery.executeQuery();
        return personFromResultSet(res, decrypt);
    }

    private static Person personFromResultSet(ResultSet res, boolean decrypt) throws SQLException {
        try {
            Person person;
            if (res.next()) {
                PreparedStatement roleQuery = prepareStatement("SELECT * FROM Role WHERE personId=?");

                int id = res.getInt(1);
                roleQuery.setInt(1, id);
                ResultSet roles = roleQuery.executeQuery();

                Role userRole = Role.USER;
                // get the highest priviledge role this user has and use that
                while (roles.next()) {
                    Role roleValue = Role.valueOf(roles.getString(2));
                    if (roleValue.getLevel() > userRole.getLevel())
                        userRole = roleValue;
                }

                // By default, if an integer value is null, JDBC returns 0 - we want it to be -1
                int paymentID = res.getInt(8);
                if (res.wasNull()) {
                    paymentID = -1;
                }

                person = new Person(
                        id,                            // id
                        res.getString(2),   // forename
                        res.getString(3),   // surname
                        res.getString(4),   // email
                        res.getString(5),   // password (this is horrible)
                        res.getString(6),   // houseNumber
                        res.getString(7),   // postcode
                        paymentID,      // bank details
                        userRole,
                        decrypt
                );

                return person;
            } else {
                return null;
            }
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a Person object into the database
     * @param person A non-null person that doesn't have any null fields either
     * @return Whether the insertion was successful or failed due to someone already having that email address
     * @throws SQLException
     */
    public static Boolean createPerson(Person person) throws SQLException {
        // do not insert this person if an account with their email already exists
        if (getPersonByEmail(person.getEmail()) != null) {
            return false;
        }

        setAutoCommit(false);
        int id = -1;

        try (PreparedStatement s = prepareStatement("INSERT INTO Person VALUES (default,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
             PreparedStatement r = prepareStatement("INSERT INTO Role VALUES (?,?)");
        ) {
            Object[] fields = person.getFields().toArray();

            s.setString(1, (String) fields[0]); // forename
            s.setString(2, (String) fields[1]); // surname
            s.setString(3, (String) fields[2]); // email
            s.setString(4, (String) fields[3]); // password
            s.setString(5, (String) fields[4]); // housename
            s.setString(6, (String) fields[5]); // postcode
            if ((Integer)fields[6] != -1) {                  // paymentid
                s.setInt(7, (Integer) fields[6]);
            } else {
                s.setNull(7, Types.INTEGER);
            }
            s.executeUpdate();

            ResultSet rs = s.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            } else {
                throw new InternalError("Failed to insert into Person table");
            }

            r.setInt(1, id);
            r.setString(2, person.getRole().name());
            r.executeUpdate();

            commit();

            person.reloadAddress();
            person.personID = id;
        } catch (SQLException | InternalError e) {
            DatabaseBridge.databaseError("Failed to insert new user", e);
            rollback();
            throw e;
        } finally {
            setAutoCommit(true);
        }

        return true;
    }

    /**
     * Assuming you already have the person entity you can update their role
     * @param person Person entity
     * @param newRole Either USER, STAFF or MANAGER
     * @return whether operation was successful
     * @throws SQLException
     */
    public static boolean updateUserRole(Person person, Role newRole) throws SQLException {
        try (PreparedStatement query = prepareStatement("""
                UPDATE Role, Person
                LEFT JOIN Role R ON Person.PersonId = R.personId
                SET R.role = ?
                WHERE Person.PersonId = ?
                """)) {
            query.setString(1, newRole.toString());
            query.setInt(2, person.getId());

            return query.executeUpdate() > 0;
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to update user with email ["+person.getEmail()+"] to role ["+newRole.toString()+"]", e);
            throw e;
        }
    }

    @Override
    public List<Object> getFields() {
        // note that the AUTO_INCREMENT primary key is not included, this method is used for inserting
        // these also appear in the same order as the table fields (important)
        List<Object> list = Arrays.asList(
                forename,
                surname,
                email,
                password,
                houseNumber,
                postCode,
                bankDetailsID
        );
        return list;
    }

    public static void validatePersonalDetails(
            String forename,
            String surname,
            String email,
            String houseNumber,
            String streetName,
            String cityName,
            String postCode
    ) throws IllegalArgumentException {
        if (forename.isEmpty() || forename == null) {
            throw new IllegalArgumentException("Forename is a compulsory field");
        }
        if (surname.isEmpty() || surname == null) {
            throw new IllegalArgumentException("Surname is a compulsory field");
        }
        if (email.isEmpty() || email == null) {
            throw new IllegalArgumentException("Email is a compulsory field");
        }
        if (houseNumber.isEmpty() || houseNumber == null) {
            throw new IllegalArgumentException("House number is a compulsory field");
        }
        if (streetName.isEmpty() || streetName == null) {
            throw new IllegalArgumentException("Street name is a compulsory field");
        }
        if (cityName.isEmpty() || cityName == null) {
            throw new IllegalArgumentException("City name is a compulsory field");
        }
        if(!Address.validatePostcode(postCode)){
            throw new IllegalArgumentException("Invalid postcode");
        }
        if(!validateEmail(email)){
            throw new IllegalArgumentException("Invalid email");
        }
    }

    public void updatePersonalDetails(
            String email,
            String forename,
            String surname,
            String houseNumber,
            String streetName,
            String cityName,
            String postCode
    ) throws SQLException, IllegalArgumentException {

        validatePersonalDetails(forename, surname, email, houseNumber, streetName, cityName, postCode);

        if (Address.getAddressById(houseNumber, postCode) == null) {
            Address.CreateAddress(new Address(houseNumber, streetName, cityName, postCode));
        } else {
            PreparedStatement s = prepareStatement("UPDATE Address SET houseNumber=?, streetName=?, cityName=?, postCode=? WHERE houseNumber=? AND postCode=?");
            s.setString(1, houseNumber);
            s.setString(2, streetName);
            s.setString(3, cityName);
            s.setString(4, postCode);
            s.setString(5, houseNumber);
            s.setString(6, postCode);
            s.executeUpdate();
        }
        PreparedStatement s = prepareStatement("UPDATE Person SET email=?, forename=?, surname=?, houseName=?, postCode=? WHERE PersonId=?");
        s.setString(1, email);
        s.setString(2, forename);
        s.setString(3, surname);
        s.setString(4, houseNumber);
        s.setString(5, postCode);
        s.setInt(6, personID);
        s.executeUpdate();
    }

    public void addNewBankDetails(BankDetail bankDetail) throws SQLException {
        if (bankDetailsID != -1) {
            PreparedStatement query = prepareStatement("DELETE FROM BankDetails WHERE paymentId = ?");
            query.setInt(1, bankDetailsID);
            query.execute();
        }

        PreparedStatement s = prepareStatement("UPDATE Person SET paymentId=? WHERE PersonId=?");
        s.setInt(1, bankDetail.getBankDetailID());
        s.setInt(2, personID);
        s.executeUpdate();

        bankDetailsID = bankDetail.getBankDetailID();
        this.bankDetail = bankDetail;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        try {
            openConnection();
            PreparedStatement s = prepareStatement("SELECT orderId FROM `Order` WHERE personId = ? ORDER BY date DESC");
            s.setInt(1, personID);

            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                orders.add(Order.getOrderWithID(rs.getInt("orderId")));
            }

            return orders;
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Error fetching all orders for user ["+personID+"]");
            throw new RuntimeException(e);
        }
    }

    public static boolean validateEmail(String email) {
        return email.matches("^[a-zA-Z0-9_+&*-]+(?:\\."+ "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z" + "A-Z]{2,7}$");
    }

    public static boolean validatePassword(String password) {
        return true; // for development
//        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!()]).{8,20}$");
    }
}

