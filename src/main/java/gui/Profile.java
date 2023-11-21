package gui;

import javax.swing.*;
import java.awt.*;

public class Profile extends JPanel{
    private JPanel profilePanel;
    private JLabel profileLabel;
    private JLabel forenameLabel;
    private JTextField forename;
    private JTextField surname;
    private JLabel surnameLabel;
    private JLabel emailLabel;
    private JLabel houseNumLabel;
    private JTextField houseNum;
    private JLabel streetNameLabel;
    private JTextField streetName;
    private JLabel cityNameLabel;
    private JTextField cityName;
    private JLabel postCodeLabel;
    private JTextField postCode;
    private JButton editDetailsButton;
    private JLabel addressLabel;
    private JLabel email;

    public Profile() {
        add(profilePanel);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        Font currentFont = profileLabel.getFont();
        Font newFont = currentFont.deriveFont(currentFont.getSize() + 5.0f).deriveFont(Font.BOLD); // Increase size by 5 points
        profileLabel.setFont(newFont);
        addressLabel.setFont(newFont);
    }

//    public static void main(String[] args) {
//        JFrame frame = new JFrame("Profile");
//        frame.setContentPane(new Profile().profilePanel);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
////      Make it full screen
//        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        frame.pack();
//        frame.setVisible(true);
//    }
}