package com.chatbot.GLUE.project.data;

import java.util.HashMap;
import java.util.Map;

public class CarbonData {
    private Map<String, Double> transportData = new HashMap<>(); // Phương tiện và quãng đường
    private double electricityUsage; // Số điện tiêu thụ (kWh)
    private double gasUsage; // Lượng gas tiêu thụ (kg)
    private double solidWaste; // Lượng chất thải rắn (kg)
    private Map<String, Integer> shoppingItems = new HashMap<>(); // Số lượng sản phẩm mua sắm
    private Map<String, Double> waterUsage = new HashMap<>(); // Sử dụng nước (nước lọc, nước thải, bia, rượu)
    private Map<String, Double> foodConsumption = new HashMap<>(); // Tiêu thụ thực phẩm (thịt bò, thịt lợn, thịt gà)

    // Getters and setters
    public Map<String, Double> getTransportData() {
        return transportData;
    }

    public void setTransportData(String vehicleType, double distance) {
        this.transportData.put(vehicleType, distance);
    }

    public double getElectricityUsage() {
        return electricityUsage;
    }

    public void setElectricityUsage(double electricityUsage) {
        this.electricityUsage = electricityUsage;
    }

    public double getGasUsage() {
        return gasUsage;
    }

    public void setGasUsage(double gasUsage) {
        this.gasUsage = gasUsage;
    }

    public double getSolidWaste() {
        return solidWaste;
    }

    public void setSolidWaste(double solidWaste) {
        this.solidWaste = solidWaste;
    }

    public Map<String, Integer> getShoppingItems() {
        return shoppingItems;
    }

    public void addShoppingItem(String item, int quantity) {
        this.shoppingItems.put(item, quantity);
    }

    public Map<String, Double> getWaterUsage() {
        return waterUsage;
    }

    public void setWaterUsage(String type, double amount) {
        this.waterUsage.put(type, amount);
    }

    public Map<String, Double> getFoodConsumption() {
        return foodConsumption;
    }

    public void setFoodConsumption(String type, double amount) {
        this.foodConsumption.put(type, amount);
    }
}