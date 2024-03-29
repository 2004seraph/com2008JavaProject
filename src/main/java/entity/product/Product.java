package entity.product;

import db.DatabaseBridge;
import db.DatabaseOperation;
import db.DatabaseRecord;
import org.javatuples.Pair;

import javax.xml.transform.Result;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Product extends DatabaseOperation.Entity implements DatabaseRecord {
    public static class ProductIsNotComponentException extends RuntimeException {
        public ProductIsNotComponentException(String msg) {super(msg); }
    }
    public static class ProductIsNotBoxedSetException extends RuntimeException {
         public ProductIsNotBoxedSetException(String msg) {super(msg); }
    }

    protected String productCode;
    protected String name;
    protected Integer stockLevel;
    protected Double price;

    public String getProductCode() {
        return productCode;
    }
    public String getName() {
        return name;
    }
    public Integer getStockLevel() {
        return stockLevel;
    }
    public Double getPrice() {
        return price;
    }

    public Product(String name, int stock, Double price) {
        this.name = name;
        this.stockLevel = stock;
        this.price = price;
    }

    private Product(String productCode, String name, int stock, Double price) {
        this.productCode = productCode;
        this.name = name;
        this.stockLevel = stock;
        this.price = price;
    }

    /**
     * Update the stock level of a product given its product code
     * @param productCode product to update
     * @param newStock new stock level
     * @return whether operation was successful or not
     * @throws SQLException
     */
    public static boolean updateStock(String productCode, int newStock) throws SQLException {
        try (PreparedStatement query = prepareStatement("UPDATE Product SET stockLevel = ? WHERE productCode = ?")) {
            query.setInt(1, newStock);
            query.setString(2, productCode);

            int updatedRows = query.executeUpdate();
            return updatedRows > 0;
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to update stock with product code ["+productCode+"]", e);
            throw e;
        }
    }

    /**
     * Returns a Product instance from the database using a product code
     * @param productCode The string product code
     * @return An instance of Product containing the field data
     * @throws SQLException
     */
    public static Product getProductByID(String productCode) throws SQLException {
        try {
            PreparedStatement query = prepareStatement("SELECT * FROM Product WHERE productCode = ?");
            query.setString(1, productCode);

            ResultSet res = query.executeQuery();
            if (res.next()) {
                return new Product(
                        res.getString("productCode"),
                        res.getString("name"),
                        res.getInt("stockLevel"),
                        res.getDouble("price")
                );
            } else {
                throw new IllegalArgumentException("Product ID does not exist");
            }
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to update stock with product code ["+productCode+"]", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all the products to display in a view
     * @return ResultSet of all products or null if there are no products
     * @throws SQLException
     */
    public static ResultSet getAllProducts() throws SQLException {
        try {
            PreparedStatement productsQuery = prepareStatement("SELECT * FROM Product");
            ResultSet results = productsQuery.executeQuery();
            if (!results.next()) {
                return null;
            }
            return results;
        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to fetch products", e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return "[Product " + productCode + " -> { Name: " + name + ", Stock: " + stockLevel + ", Price: " + price + " }]";
    }

    public boolean isBoxedSet() {
        char productTypeIdentifier = productCode.charAt(0);
        List<Character> componentCodes = Arrays.asList('M', 'P');

        return componentCodes.contains(productTypeIdentifier);
    }

    public boolean isComponent() throws SQLException {
        char productTypeIdentifier = productCode.charAt(0);
        List<Character> componentCodes = Arrays.asList('L', 'C', 'R', 'S');

        return componentCodes.contains(productTypeIdentifier);
    }

    public BoxedSet getBoxedSet() throws SQLException {
        if (!isBoxedSet()) {
            throw new ProductIsNotBoxedSetException("Tried to get boxedset of product [" + productCode + "]");
        }

        try {
            PreparedStatement q = prepareStatement("SELECT contentProductCode, quantity FROM BoxedSetContent WHERE boxSetProductCode = ?");
            q.setString(1, productCode);

            List<Pair<Component, Integer>> componentList = new ArrayList<>();
            List<Pair<BoxedSet, Integer>> boxedSetList = new ArrayList<>();

            ResultSet rs = q.executeQuery();
            while (rs.next()) {
                // This is stupid
                int quantity = rs.getInt("quantity");
                Product product = getProductByID(rs.getString("contentProductCode"));
                if (product.isComponent()) {
                    Component component = product.getComponent();
                    componentList.add(new Pair<>(component, quantity));
                } else {
                    BoxedSet boxedSet = product.getBoxedSet();
                    boxedSetList.add(new Pair<>(boxedSet, quantity));
                }
            }

            return new BoxedSet(name, stockLevel, price, componentList, boxedSetList);

        } catch (SQLException e) {
            DatabaseBridge.databaseError("Failed to fetch boxed-set components for set ["+productCode+"]");
            throw e;
        }
    }

    public Component getComponent() throws SQLException {
        if (!isComponent()) {
            throw new ProductIsNotComponentException("Tried to get component of product [" + productCode + "]");
        }

        // First char of product code identifies product type
        char productTypeIdentifier = productCode.charAt(0);

        switch (productTypeIdentifier) {
            case 'L':
                try {
                    PreparedStatement q = prepareStatement("""
                        SELECT brand, era, gauge, priceBracket
                        FROM Component
                                 LEFT OUTER JOIN Locomotive on Component.productCode = Locomotive.productCode
                        WHERE Component.productCode = ?""");
                    q.setString(1, productCode);

                    ResultSet rs = q.executeQuery();
                    if (!rs.next()) {
                        throw new Component.ComponentNotFoundException("Could not find locomotive with product code ["+productCode+"]");
                    }

                    return new Locomotive(
                            name,
                            stockLevel,
                            price,
                            rs.getString("brand"),
                            rs.getString("era"),
                            Component.Gauge.valueOf(rs.getString("gauge")),
                            Locomotive.PriceBracket.get(rs.getString("priceBracket"))
                            );
                } catch (SQLException e) {
                    DatabaseBridge.databaseError("Failed to fetch locomotive with product code ["+productCode+"]", e);
                    throw e;
                }
            case 'C':
                try {
                    PreparedStatement q = prepareStatement("""
                        SELECT brand, era, gauge, controlType
                        FROM Component
                                 LEFT OUTER JOIN Controller on Component.productCode = Controller.productCode
                        WHERE Component.productCode = ?""");
                    q.setString(1, productCode);

                    ResultSet rs = q.executeQuery();
                    if (!rs.next()) {
                        throw new Component.ComponentNotFoundException("Could not find controller with product code ["+productCode+"]");
                    }

                    return new Controller(
                            name,
                            stockLevel,
                            price,
                            rs.getString("brand"),
                            rs.getString("era"),
                            Controller.ControlType.valueOf(rs.getString("controlType"))
                    );
                } catch (SQLException e) {
                    DatabaseBridge.databaseError("Failed to fetch controller with product code ["+productCode+"]", e);
                    throw e;
                }
            case 'R':
                try {
                    PreparedStatement q = prepareStatement("""
                        SELECT brand, era, gauge, curvature
                        FROM Component
                                 LEFT OUTER JOIN Track on Component.productCode = Track.productCode
                        WHERE Component.productCode = ?""");
                    q.setString(1, productCode);

                    ResultSet rs = q.executeQuery();
                    if (!rs.next()) {
                        throw new Component.ComponentNotFoundException("Could not find track with product code ["+productCode+"]");
                    }

                    return new Track(
                            name,
                            stockLevel,
                            price,
                            rs.getString("brand"),
                            rs.getString("era"),
                            Component.Gauge.valueOf(rs.getString("gauge")),
                            Track.Curvature.get(rs.getString("curvature"))
                    );
                } catch (SQLException e) {
                    DatabaseBridge.databaseError("Failed to fetch track with product code ["+productCode+"]", e);
                    throw e;
                }
            default:
                try {
                    PreparedStatement q = prepareStatement("SELECT brand, era, gauge FROM Component WHERE productCode = ?");
                    q.setString(1, productCode);

                    ResultSet rs = q.executeQuery();
                    if (!rs.next()) {
                        throw new Component.ComponentNotFoundException("Could not find component with product code ["+productCode+"]");
                    }

                    return new Component(
                            name,
                            stockLevel,
                            price,
                            rs.getString("brand"),
                            rs.getString("era"),
                            Component.Gauge.valueOf(rs.getString("gauge"))
                    );
                } catch (SQLException e) {
                    DatabaseBridge.databaseError("Failed to fetch component with product code ["+productCode+"]", e);
                    throw e;
                }

        }
    }

    @Override
    public List<Object> getFields() {
        return Arrays.asList(
                name,
                stockLevel,
                price
        );
    }
}

