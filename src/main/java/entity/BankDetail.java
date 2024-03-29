package entity;

import controllers.AppContext;
import db.DatabaseBridge;
import db.DatabaseOperation;
import db.DatabaseRecord;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import utils.Crypto;

import java.security.InvalidKeyException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static utils.Java.createDate;

public class BankDetail extends DatabaseOperation.Entity implements DatabaseRecord {
    public static class BankAccountNotFoundException extends RuntimeException {
        public BankAccountNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidBankDetailsException extends Exception {
        public InvalidBankDetailsException(String message) {
            super(message);
        }
    }
    private int bankDetailID = -1;

    private final String cardHolderName;
    private final String cardName;
    private final String cardNumber;
    private final Date expiryDate;
    private final String securityCode;


    public int getBankDetailID() { return bankDetailID; }
    public String getCardHolderName() { return cardHolderName; }
    public String getCardName() {
        return cardName;
    }
    public String getCardNumber() {
        return cardNumber;
    }
    public Date getExpiryDate() {
        return expiryDate;
    }
    public String getSecurityCode() {
        return securityCode;
    }

    private BankDetail(int bankDetailID, String cardName, String cardHolderName, String cardNumber, Date expiryDate, String securityCode) {
        this.bankDetailID = bankDetailID;
        this.cardHolderName = cardHolderName;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.securityCode = securityCode;
    }

    /**
     * Creates an entry in the database for a new set of bank details, it also returns a constructed class representation.
     * @param cardName
     * @param cardNumber
     * @param cardHolderName
     * @param expiryDate
     * @param securityCode
     * @return A new instance of BankDetail referencing an entry in the DB
     * @throws SQLException
     * @throws InvalidBankDetailsException
     */
    public static BankDetail createPaymentInfo(String cardName, String cardNumber, String cardHolderName, Date expiryDate, String securityCode)
            throws SQLException, InvalidBankDetailsException {
        int id = -1;
        boolean isCardValid = LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(cardNumber);
        if (!isCardValid) {
            throw new InvalidBankDetailsException("Card number invalid, failed Luhn check ["+cardNumber+"]");
        }
        // Expiry date should be in the format MM/YY
        if (expiryDate.before(new Date())) {
            throw new InvalidBankDetailsException("Card is expired ["+expiryDate+"]");
        }

        if (!(securityCode.length() == 3 || securityCode.length() == 4)) {
            throw new InvalidBankDetailsException("Security code was an invalid length ["+securityCode+"]");
        }

        try (PreparedStatement cardQuery = prepareStatement("INSERT INTO BankDetails (cardName, cardHolderName, cardNumber, expiryDate, securityCode) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            byte[] encryptionKey = AppContext.getEncryptionKey();
            String encryptedCardNumber = Crypto.encryptString(cardNumber, encryptionKey);
            String encryptedSecurityCode = Crypto.encryptString(securityCode, encryptionKey);

            cardQuery.setString(1, cardName);
            cardQuery.setString(2, cardHolderName);
            cardQuery.setString(3, encryptedCardNumber);
            cardQuery.setDate(4, new java.sql.Date(expiryDate.getTime()));
            cardQuery.setString(5, encryptedSecurityCode);

            cardQuery.executeUpdate();
            ResultSet rs = cardQuery.getGeneratedKeys();

            if (rs.next()) {
                id = rs.getInt(1);
            } else {
                throw new InternalError("Failed to insert into BankDetails table");
            }

            return new BankDetail(id, cardName,cardHolderName, cardNumber, expiryDate, securityCode);
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to insert new payment info ["+cardName+"]", e);
            throw e;
        } catch (InvalidKeyException e) {
            Crypto.cryptoError("Error whilst encrypting card number, encryption key was invalid", e);
            throw new RuntimeException(e);
        }
    }

    public static BankDetail getBankDetailsById(int id) throws SQLException, InvalidKeyException {
        try(PreparedStatement bankQuery = prepareStatement("SELECT * FROM BankDetails WHERE paymentId = ?")) {
            bankQuery.setInt(1, id);
            ResultSet rs = bankQuery.executeQuery();

            if (!rs.next()) {
                throw new BankAccountNotFoundException("Failed to find bank details with id ["+id+"]");
            }

            byte[] encryptionKey = AppContext.getEncryptionKey();
            String decryptedCardNumber = Crypto.decryptString(rs.getString("cardNumber"), encryptionKey);
            String decryptedSecurityCode = Crypto.decryptString(rs.getString("securityCode"), encryptionKey);

            return new BankDetail(
                    rs.getInt("paymentId"),
                    rs.getString("cardName"),
                    rs.getString("cardHolderName"),
                    decryptedCardNumber,
                    rs.getDate("expiryDate"),
                    decryptedSecurityCode
            );
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to fetch bank details with id ["+id+"]", e);
            throw e;
        } catch (InvalidKeyException e) {
            // Thrown when the user tries to decrypt another user's card with an invalid encryption key
            // I.e. they are trying to decrypt a card that isn't theirs
            Crypto.cryptoError("User tried to decrypt card with id ["+id+"] but they used the wrong encryption key", e);
            throw e;
        }
    }

    public static void validateBankDetails(String cardName, String cardNumber, String cardHolderName, String expiryDate, String securityCode)
            throws InvalidBankDetailsException{
        if (cardNumber == null || expiryDate == null || securityCode == null || cardHolderName == null || cardName == null) {
            throw new InvalidBankDetailsException("Please enter a valid card number, expiry date and security code");
        }
        boolean isCardValid = LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(cardNumber); // 4012888888881881
        if (!isCardValid) {
            throw new InvalidBankDetailsException("Card number invalid, failed Luhn check ["+cardNumber+"]");
        }
        // Expiry date should be in the format yyyy-MM-dd
        if (!expiryDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) { //2024-01-01
            throw new InvalidBankDetailsException("Expiry date was an invalid format");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate exDate = LocalDate.parse(expiryDate, formatter);

        if (exDate.isBefore(LocalDate.now())) {
            throw new InvalidBankDetailsException("Card is expired ["+expiryDate+"]");
        }
//        Check if card is expired

        if (!(securityCode.length() == 3 || securityCode.length() == 4)) {
            throw new InvalidBankDetailsException("Security code was an invalid length ["+securityCode+"]");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(cardName);
        sb.append(", ");
        sb.append(cardNumber.substring(12)); // last 4 digits
        return sb.toString();
    }

    @Override
    public List<Object> getFields() {
        return Arrays.asList(
                cardName,
                cardNumber,
                expiryDate,
                securityCode
        );
    }
}