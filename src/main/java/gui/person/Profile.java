package gui.person;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.sql.SQLException;

import db.DatabaseBridge;
import db.DatabaseOperation;
import db.DatabaseRecord;
import entity.BankDetail;
import entity.user.Person;
import controllers.AppContext;
import gui.App;

public class Profile extends JPanel{

//    Personal Details
    private JLabel forenameLabel = new JLabel("Forename:");
    private JTextField forename = new JTextField(30);
    private JLabel surnameLabel = new JLabel("Surname:");
    private JTextField surname = new JTextField(30);
    private JLabel emailLabel = new JLabel("Email:");
    private JLabel email = new JLabel();
//    Address
    private JLabel houseNumberLabel = new JLabel("House Number:");
    private JTextField houseNumber = new JTextField(30);
    private JLabel streetLabel = new JLabel("Street:");
    private JTextField street = new JTextField(30);
    private JLabel cityLabel = new JLabel("City:");
    private JTextField city = new JTextField(30);
    private JLabel postCodeLabel = new JLabel("PostCode:");
    private JTextField postCode = new JTextField(30);
//    Bank Details
    private JLabel cardNumberLabel = new JLabel("Card Number:");
    private JTextField cardNumber = new JTextField(30);
    private JLabel expiryDateLabel = new JLabel("Expiry Date:");
    private JTextField expiryDate = new JTextField(30);
    private JLabel securityCodeLabel = new JLabel("Security Code:");
    private JTextField securityCode = new JTextField(30);
//    Buttons
    private JButton updateButton = new JButton("UPDATE");
    private JButton addBankDetailsButton = new JButton("ADD BANK DETAILS");

    private BankDetail bankDetail;

    public Profile() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 10, 1, 1);


//        Add the title
        JLabel title = new JLabel("<html><h1>Profile</h1></html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        add(title, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        addField(gbc, forenameLabel, forename, 1);
        addField(gbc, surnameLabel, surname, 2);

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(emailLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(email, gbc);

        JLabel addressTitle = new JLabel("<html><h2>Address</h2></html>");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(addressTitle, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        addField(gbc, houseNumberLabel, houseNumber, 5);
        addField(gbc, streetLabel, street, 6);
        addField(gbc, cityLabel, city, 7);
        addField(gbc, postCodeLabel, postCode, 8);;



        Person person = AppContext.getCurrentUser();
        DatabaseBridge db = DatabaseBridge.instance();
        //            TODO: HANDLE BANK DETAILS IF NULL
        try {
            db.openConnection();
            forename.setText(person.getForename());
            surname.setText(person.getSurname());
            email.setText(person.getEmail());
            houseNumber.setText(person.getAddress().getHouseNumber());
            street.setText(person.getAddress().getStreetName());
            city.setText(person.getAddress().getCityName());
            postCode.setText(person.getAddress().getPostcode());

            if (person.getBankDetail() != null) {
//                Add bank details to the profile
//                Add bank details title
                JLabel bankDetailsTitle = new JLabel("<html><h2>Bank Details</h2></html>");
                gbc.gridx = 0;
                gbc.gridy = 9;
                gbc.gridwidth = 2;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.CENTER;
                add(bankDetailsTitle, gbc);

                gbc.fill = GridBagConstraints.NONE;
                gbc.anchor = GridBagConstraints.WEST;
                addField(gbc, cardNumberLabel, cardNumber, 10);
                addField(gbc, expiryDateLabel, expiryDate, 11);
                addField(gbc, securityCodeLabel, securityCode, 12);
                cardNumber.setText(person.getBankDetail().getCardNumber());
                expiryDate.setText(person.getBankDetail().getExpiryDate().toString());
                securityCode.setText(person.getBankDetail().getSecurityCode());

//                For the update button
                gbc.gridx = 0;
                gbc.gridy = 13;
                gbc.gridwidth = 2;
                gbc.fill = GridBagConstraints.NONE;
                gbc.anchor = GridBagConstraints.CENTER;
            } else {
                gbc.gridx = 1;
                gbc.gridy = 10;
                add(addBankDetailsButton, gbc);
//                For the update button
                gbc.gridx = 0;
                gbc.gridy = 10;

                addBankDetailsButton.addActionListener(e -> {
                    getBankDetailsFromUser();
                });
            }
            add(updateButton, gbc);
            updateButton.addActionListener(e -> {
                updateDetails();
            });

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            db.closeConnection();
        }
    }

    private void addField(GridBagConstraints gbc, JLabel label, JTextField textField, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        add(label, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        add(textField, gbc);
    }


    /**
     * Updates the user's personal details
     */
    public void updateDetails(){
        String forenameInput = forename.getText();
        String surnameInput = surname.getText();
        String houseNumberInput = houseNumber.getText();
        String streetInput = street.getText();
        String cityInput = city.getText();
        String postCodeInput = postCode.getText();
//        Validate the data
        try {
            Person.validatePersonalDetails(forenameInput, surnameInput, houseNumberInput, streetInput, cityInput, postCodeInput);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(AppContext.getWindow(), e.getMessage());
        }
        DatabaseBridge db = DatabaseBridge.instance();
        try {
            db.openConnection();
            Person.updatePersonalDetails(forenameInput, surnameInput, houseNumberInput, streetInput, cityInput, postCodeInput);
            JOptionPane.showMessageDialog(AppContext.getWindow(), "Personal Details Updated");
        } catch (Exception exception) {
        } finally {
            db.closeConnection();
        }

    }


    /**
     * Creates a new JFrame as a pop up that allows the user to enter their bank details
     */
    public void getBankDetailsFromUser() {
        // Create a new JFrame
        JDialog frame = new JDialog(AppContext.getWindow());
        frame.setTitle("Bank Details");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Create the labels
        JLabel cardNumberLabel = new JLabel("Card Number");
        JLabel expiryDateLabel = new JLabel("Expiry Date (yyyy-mm-dd)");
        JLabel securityCodeLabel = new JLabel("Security Code");

        // Create the text fields
        JTextField cardNumber = new JTextField(25);
        JTextField expiryDate = new JTextField(25);
        JTextField securityCode = new JTextField(25);

        // Create the button
        JButton submitButton = new JButton("Submit");

//        Add the title
        JLabel title = new JLabel("<html><h1>Bank Details</h1></html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        frame.add(title, gbc);

        // Add the labels and text fields to the JFrame
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(cardNumberLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        frame.add(cardNumber, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(expiryDateLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        frame.add(expiryDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        frame.add(securityCodeLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        frame.add(securityCode, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        frame.add(submitButton, gbc);

        frame.setVisible(true);

        // When clicking submitButton, close this window and open Register window
        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cardNumberInput = cardNumber.getText();
                String expiryDateInput = expiryDate.getText();
                String securityCodeInput = securityCode.getText();
                try{
                   BankDetail.validateBankDetails(cardNumberInput, expiryDateInput, securityCodeInput);
                } catch (BankDetail.InvalidBankDetailsException exception) {
                    JOptionPane.showMessageDialog(AppContext.getWindow(), exception.getMessage());
                }
                Date expiryDate = Date.valueOf(expiryDateInput);
                DatabaseBridge db = DatabaseBridge.instance();
                try{
                    db.openConnection();
                    bankDetail = BankDetail.createPaymentInfo(cardNumberInput, expiryDate, securityCodeInput);
//                    Add the bank details to the user
                    Person.addBankDetailsToPerson(bankDetail);
                    JOptionPane.showMessageDialog(AppContext.getWindow(), "Bank Details Added");
                } catch (BankDetail.InvalidBankDetailsException exception) {
                    JOptionPane.showMessageDialog(AppContext.getWindow(), exception.getMessage());
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                } finally {
                    db.closeConnection();
                    frame.dispose();
                }
                try {
                    db.openConnection();
                    AppContext.setCurrentUser(Person.getPersonByID(AppContext.getCurrentUser().getId()));
                    System.out.println(AppContext.getCurrentUser().getBankDetail().getCardName());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                } finally {
                    db.closeConnection();
                }
                revalidate();
                repaint();
            }
        });

    }


}
