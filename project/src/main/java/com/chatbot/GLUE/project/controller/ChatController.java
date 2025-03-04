package com.chatbot.GLUE.project.controller;

import com.chatbot.GLUE.project.data.CarbonData;
import com.chatbot.GLUE.project.data.ConversationState;
import com.chatbot.GLUE.project.dto.ChatRequest;
import com.chatbot.GLUE.project.dto.ChatResponse;
import com.chatbot.GLUE.project.service.CarbonFootprintService;
import com.chatbot.GLUE.project.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private CarbonFootprintService carbonFootprintService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Lưu trữ trạng thái hội thoại và dữ liệu của người dùng
    private final Map<String, ConversationState> userStates = new HashMap<>();
    private final Map<String, CarbonData> userCarbonData = new HashMap<>();
    private final Map<String, String> tempVehicleType = new HashMap<>();
    private final Map<String, Boolean> hasGreeted = new HashMap<>(); // Theo dõi xem đã gửi câu chào chưa

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userId = "default_user"; // Trong ứng dụng thực tế, đây nên là định danh duy nhất của người dùng
        String message = request.getMessage().trim().toLowerCase();
        logger.info("Nhận được yêu cầu từ người dùng {}: {}", userId, message);

        // Khởi tạo trạng thái và dữ liệu nếu chưa có
        userStates.putIfAbsent(userId, ConversationState.IDLE);
        userCarbonData.putIfAbsent(userId, new CarbonData());
        hasGreeted.putIfAbsent(userId, false); // Khởi tạo trạng thái chào hỏi

        // Hiển thị câu chào nếu đây là lần đầu tiên tương tác
        if (!hasGreeted.get(userId)) {
            hasGreeted.put(userId, true);
            return new ChatResponse("Tôi là trợ lý CO2 ảo, nếu bạn muốn tính CO2 thì nhắn 'tính toán co2' hoặc bạn có thể hỏi bất cứ điều gì bạn muốn");
        }

        ConversationState state = userStates.get(userId);
        CarbonData data = userCarbonData.get(userId);

        String response;
        if (message.equals("tính toán co2")) {
            userStates.put(userId, ConversationState.ASK_TRANSPORT);
            response = "Hàng ngày bạn đi học/đi làm bằng phương tiện gì? (Ví dụ: đi bộ, xe đạp, xe máy, ô tô, xe bus, tàu hỏa, máy bay...)";
        } else {
            switch (state) {
                case IDLE:
                    try {
                        response = geminiService.callApi(request.getMessage(), geminiApiKey);
                    } catch (Exception e) {
                        logger.error("Lỗi khi gọi API Gemini", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xử lý yêu cầu", e);
                    }
                    break;

                case ASK_TRANSPORT:
                    tempVehicleType.put(userId, message);
                    userStates.put(userId, ConversationState.ASK_DISTANCE);
                    response = "Bạn đi bao nhiêu km mỗi ngày bằng phương tiện này?";
                    break;

                case ASK_DISTANCE:
                    try {
                        double distance = Double.parseDouble(message);
                        String vehicleType = tempVehicleType.get(userId);
                        if (vehicleType == null) {
                            response = "Có lỗi xảy ra, vui lòng bắt đầu lại. Hàng ngày bạn đi học/đi làm bằng phương tiện gì?";
                            userStates.put(userId, ConversationState.ASK_TRANSPORT);
                        } else {
                            data.setTransportData(vehicleType, distance);
                            userStates.put(userId, ConversationState.ASK_ELECTRICITY);
                            response = "Hàng ngày bạn sử dụng trung bình khoảng bao nhiêu số điện (kWh)? (Nếu không biết, hãy trả lời 'không biết')";
                        }
                    } catch (NumberFormatException e) {
                        response = "Vui lòng nhập số km hợp lệ (ví dụ: 10.5). Bạn đi bao nhiêu km mỗi ngày?";
                    }
                    break;

                case ASK_ELECTRICITY:
                    if (message.equals("không biết")) {
                        response = "Bạn dùng TV bao nhiêu giờ mỗi ngày? Và dùng đèn điện bao nhiêu giờ? (Trả lời dạng: TV 2 giờ, đèn 5 giờ)";
                    } else {
                        try {
                            double electricity = Double.parseDouble(message);
                            data.setElectricityUsage(electricity);
                            userStates.put(userId, ConversationState.ASK_GAS);
                            response = "Bạn dùng loại bếp gì khi nấu ăn? Bạn sử dụng bao nhiêu kg gas mỗi ngày? (Nếu không dùng gas, trả lời 'không dùng')";
                        } catch (NumberFormatException e) {
                            response = "Vui lòng nhập số hợp lệ (ví dụ: 5.5). Bạn sử dụng bao nhiêu số điện (kWh)?";
                        }
                    }
                    break;

                case ASK_GAS:
                    if (message.equals("không dùng")) {
                        data.setGasUsage(0);
                    } else {
                        try {
                            double gas = Double.parseDouble(message.split(" ")[0]); // Phân tích đơn giản
                            data.setGasUsage(gas);
                        } catch (Exception e) {
                            response = "Vui lòng nhập số hợp lệ (ví dụ: 0.5). Bạn sử dụng bao nhiêu kg gas mỗi ngày?";
                            break;
                        }
                    }
                    userStates.put(userId, ConversationState.ASK_WASTE);
                    response = "Lượng chất thải rắn hàng ngày của bạn ước lượng là bao nhiêu (kg)? (Ví dụ: 1 kg)";
                    break;

                case ASK_WASTE:
                    try {
                        double waste = Double.parseDouble(message);
                        data.setSolidWaste(waste);
                        userStates.put(userId, ConversationState.ASK_SHOPPING);
                        response = "Hôm nay bạn mua bao nhiêu túi nhựa, chai nhựa, bộ quần áo, điện thoại, laptop? (Trả lời dạng: túi nhựa 2, chai nhựa 1, quần áo 0,...)";
                    } catch (NumberFormatException e) {
                        response = "Vui lòng nhập số hợp lệ (ví dụ: 1). Lượng chất thải rắn hàng ngày của bạn là bao nhiêu (kg)?";
                    }
                    break;

                case ASK_SHOPPING:
                    parseShoppingItems(message, data);
                    userStates.put(userId, ConversationState.ASK_WATER);
                    response = "Bạn sử dụng bao nhiêu nước lọc (m3)/nước thải (m3)/bia (lít)/rượu (lít) mỗi ngày? (Trả lời dạng: nước lọc 0.5, bia 1,...)";
                    break;

                case ASK_WATER:
                    parseWaterUsage(message, data);
                    userStates.put(userId, ConversationState.ASK_FOOD);
                    response = "Bạn tiêu thụ bao nhiêu thịt bò, thịt lợn, thịt gà mỗi ngày (kg)? (Trả lời dạng: thịt bò 0.2, thịt lợn 0.5,...)";
                    break;

                case ASK_FOOD:
                    parseFoodConsumption(message, data);
                    userStates.put(userId, ConversationState.CALCULATE);
                    double totalCO2 = carbonFootprintService.calculateTotalCarbonFootprint(data);
                    String suggestions = carbonFootprintService.getSuggestions(data);
                    response = String.format("Tổng lượng CO2 của bạn hôm nay là: %.2f kg.\n%s", totalCO2, suggestions);
                    userStates.put(userId, ConversationState.IDLE); // Đặt lại trạng thái
                    userCarbonData.remove(userId); // Xóa dữ liệu
                    break;

                default:
                    response = "Đã có lỗi xảy ra. Vui lòng thử lại.";
                    userStates.put(userId, ConversationState.IDLE);
                    break;
            }
        }

        return new ChatResponse(response);
    }

    @GetMapping("/test")
    public String test() {
        return "Điểm cuối thử nghiệm hoạt động!";
    }

    private void parseShoppingItems(String message, CarbonData data) {
        String[] items = message.split(",");
        for (String item : items) {
            try {
                String[] parts = item.trim().split(" ");
                String itemName = parts[0].replace(" ", "_");
                int quantity = Integer.parseInt(parts[1]);
                data.addShoppingItem(itemName, quantity);
            } catch (Exception ignored) {
            }
        }
    }

    private void parseWaterUsage(String message, CarbonData data) {
        String[] items = message.split(",");
        for (String item : items) {
            try {
                String[] parts = item.trim().split(" ");
                String type = parts[0].replace(" ", "_");
                double amount = Double.parseDouble(parts[1]);
                data.setWaterUsage(type, amount);
            } catch (Exception ignored) {
            }
        }
    }

    private void parseFoodConsumption(String message, CarbonData data) {
        String[] items = message.split(",");
        for (String item : items) {
            try {
                String[] parts = item.trim().split(" ");
                String type = parts[0].replace(" ", "_");
                double amount = Double.parseDouble(parts[1]);
                data.setFoodConsumption(type, amount);
            } catch (Exception ignored) {
            }
        }
    }
}