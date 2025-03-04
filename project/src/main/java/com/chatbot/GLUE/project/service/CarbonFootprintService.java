package com.chatbot.GLUE.project.service;

import com.chatbot.GLUE.project.data.CarbonData;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CarbonFootprintService {

    private final Map<String, Double> transportEmissionFactors = new HashMap<>();
    private final Map<String, Double> shoppingEmissionFactors = new HashMap<>();
    private final Map<String, Double> waterEmissionFactors = new HashMap<>();
    private final Map<String, Double> foodEmissionFactors = new HashMap<>();

    public CarbonFootprintService() {
        // Transport emission factors (kg CO2/km)
        transportEmissionFactors.put("walking", 0.0);
        transportEmissionFactors.put("bicycle", 0.0);
        transportEmissionFactors.put("electric", 0.011);
        transportEmissionFactors.put("motorbike_50cc", 0.070);
        transportEmissionFactors.put("motorbike_125cc", 0.081);
        transportEmissionFactors.put("motorbike_large", 0.098);
        transportEmissionFactors.put("car", 0.140);
        transportEmissionFactors.put("bus", 0.101);
        transportEmissionFactors.put("truck", 0.195);
        transportEmissionFactors.put("container", 0.586);
        transportEmissionFactors.put("train", 0.035);
        transportEmissionFactors.put("plane", 0.2);

        // Shopping emission factors (kg CO2/unit)
        shoppingEmissionFactors.put("plastic_bag", 0.033);
        shoppingEmissionFactors.put("plastic_bottle", 0.083);
        shoppingEmissionFactors.put("clothing", 2.0);
        shoppingEmissionFactors.put("phone", 70.0);
        shoppingEmissionFactors.put("laptop", 200.0);

        // Water usage emission factors (kg CO2/unit)
        waterEmissionFactors.put("clean_water", 0.79); // per m3
        waterEmissionFactors.put("wastewater", 1.14); // per m3
        waterEmissionFactors.put("beer", 0.81); // per liter
        waterEmissionFactors.put("wine", 2.16); // per liter

        // Food consumption emission factors (kg CO2/kg)
        foodEmissionFactors.put("beef", 24.0);
        foodEmissionFactors.put("pork", 10.0);
        foodEmissionFactors.put("chicken", 3.0);
    }

    public double calculateTotalCarbonFootprint(CarbonData data) {
        double totalCO2 = 0.0;

        // Transport
        for (Map.Entry<String, Double> entry : data.getTransportData().entrySet()) {
            String vehicleType = entry.getKey();
            double distance = entry.getValue();
            double emissionFactor = transportEmissionFactors.getOrDefault(vehicleType, 0.0);
            totalCO2 += emissionFactor * distance;
        }

        // Electricity
        totalCO2 += 0.57 * data.getElectricityUsage();

        // Gas usage (cooking)
        totalCO2 += 3.001 * data.getGasUsage();

        // Solid waste
        totalCO2 += 0.042 * data.getSolidWaste();

        // Shopping
        for (Map.Entry<String, Integer> entry : data.getShoppingItems().entrySet()) {
            String item = entry.getKey();
            int quantity = entry.getValue();
            double emissionFactor = shoppingEmissionFactors.getOrDefault(item, 0.0);
            totalCO2 += emissionFactor * quantity;
        }

        // Water usage
        for (Map.Entry<String, Double> entry : data.getWaterUsage().entrySet()) {
            String type = entry.getKey();
            double amount = entry.getValue();
            double emissionFactor = waterEmissionFactors.getOrDefault(type, 0.0);
            totalCO2 += emissionFactor * amount;
        }

        // Food consumption
        for (Map.Entry<String, Double> entry : data.getFoodConsumption().entrySet()) {
            String type = entry.getKey();
            double amount = entry.getValue();
            double emissionFactor = foodEmissionFactors.getOrDefault(type, 0.0);
            totalCO2 += emissionFactor * amount;
        }

        return totalCO2;
    }

    // Provide suggestions for reducing carbon footprint
    public String getSuggestions(CarbonData data) {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("Dưới đây là một số gợi ý để giảm lượng CO2 của bạn:\n");

        // Transport suggestions
        if (data.getTransportData().containsKey("car") || data.getTransportData().containsKey("motorbike_large")) {
            suggestions.append("- Giao thông: Hãy thử sử dụng phương tiện công cộng hoặc xe đạp để giảm khoảng 50% lượng khí thải từ di chuyển.\n");
        }

        // Electricity suggestions
        if (data.getElectricityUsage() > 5) { // Arbitrary threshold for high usage
            suggestions.append("- Tiêu thụ điện: Tắt điều hòa khi không sử dụng và thay bằng quạt để tiết kiệm khoảng 0.5 kg CO2 mỗi giờ.\n");
            suggestions.append("  Hoặc chuyển sang bóng đèn LED để giảm khoảng 20% lượng khí thải từ điện năng hàng tháng.\n");
        }

        // Food consumption suggestions
        if (data.getFoodConsumption().containsKey("beef") && data.getFoodConsumption().get("beef") > 0) {
            suggestions.append("- Thực phẩm: Giảm tiêu thụ thịt đỏ 1 ngày/tuần, bạn có thể cắt giảm khoảng 24 kg CO2 mỗi kg thịt bò không tiêu thụ.\n");
        }

        // Shopping suggestions
        if (data.getShoppingItems().containsKey("plastic_bag") && data.getShoppingItems().get("plastic_bag") > 0) {
            suggestions.append("- Mua sắm: Sử dụng túi vải thay túi nhựa để giảm khoảng 0.033 kg CO2 mỗi lần sử dụng.\n");
        }

        return suggestions.toString();
    }
}