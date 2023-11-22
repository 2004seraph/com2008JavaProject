package gui;

import controllers.AppContext;
import entity.StoreAttributes;

import javax.swing.*;
import java.awt.*;

public class App {
    private final TabbedGUIContainer screenController;

    public App() { // THIS IS RAN ONCE
        screenController = new TabbedGUIContainer(0.2f);

        JFrame frame = AppContext.getWindow();
        frame.getContentPane().add(screenController);

        loginState();

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    public void loginState() {
        screenController.removeAllTabs();

        JPanel registerPage = new Register(this);
        JPanel loginPage = new Login(this);
        screenController.insertTab("Register", registerPage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });
        screenController.insertTab("Login", loginPage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });

        screenController.switchTab("Login");
    }

    /**
     * The logged-in screen with each dashboard this role has access too
     */
    public void userState(StoreAttributes.Role userRole) {
        screenController.removeAllTabs();
        switch (userRole) {
            case MANAGER:
                ManagerScreen manScreen = new ManagerScreen();
                screenController.insertDivider();
                screenController.insertTab("Management", manScreen);
                // deliberate fallthrough
            case STAFF:
                screenController.insertDivider();
                break;
        }


        JPanel shopPage = new Shop();
        JPanel ordersPage = new Orders();
        JPanel profilePage = new Profile();
        JPanel cartPage = new Cart();
        JPanel logoutPage = new Logout();

        screenController.insertTab("Logout", logoutPage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });
        screenController.insertTab("Cart", cartPage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });
        screenController.insertTab("Profile", profilePage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });
        screenController.insertTab("My Orders", ordersPage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });
        screenController.insertTab("Shop", shopPage, new TabbedGUIContainer.ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });

        screenController.switchTab("Profile");
    }
}
