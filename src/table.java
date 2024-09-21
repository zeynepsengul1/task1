import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class table {
    public static class ContractData {
        private String date;
        private String hour;
        private String contractName;
        private double price;
        private int quantity;
        private long id;

        public String getDate() {
            return date;
        }
        public void setDate(String date) {
            this.date = date;
        }
        public String getHour() {
            return hour;
        }
        public void setHour(String hour) {
            this.hour = hour;
        }
        public String getContractName() {
            return contractName;
        }
        public void setContractName(String contractName) {
            this.contractName = contractName;
        }
        public double getPrice() {
            return price;
        }
        public void setPrice(double price) {
            this.price = price;
        }
        public int getQuantity() {
            return quantity;
        }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        public long getId() {
            return id;
        }
        public void setId(long id) {
            this.id = id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private List<ContractData> items;

        public List<ContractData> getItems() {
            return items;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("EPIAS Verisi - Zeynep Sengul");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        String[] columnNames = {"Grup", "Tarih ve Saat", "Toplam İşlem Miktarı (MWh)", "Toplam İşlem Tutarı (TL)", "Ağırlık Ortalama Fiyat (TL/MWh)"};

        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);


        try {
            URL url = new URL("https://seffaflik.epias.com.tr/electricity-service/v1/markets/idm/data/transaction-history");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("TGT", "TGT-1472744-Mxu1jt3W16N0bSwS6SiVw2-jRdZLr6fOh6pVUY1V6nBEgoBHhdh4-pRps7jfEeSynFQ-cas-7bc549fccd-tfkqm");
            conn.setDoOutput(true);

            String jsonInputString = "{\"endDate\": \"2024-09-11T00:00:00+03:00\", \"startDate\": \"2024-09-10T00:00:00+03:00\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    ObjectMapper objectMapper = new ObjectMapper();
                    Response responseObject = objectMapper.readValue(response.toString(), Response.class);

                    List<ContractData> dataList = responseObject.getItems();

                    Map<String, Double> groupCalculationsOne = new HashMap<>();
                    Map<String, Double> groupCalculationsTwo = new HashMap<>();
                    Map<String, Double> groupCalculations = new HashMap<>();

                    for (ContractData data : dataList) {
                        String key = data.getContractName().substring(0, 10);

                        groupCalculationsOne.put(key, groupCalculationsOne.getOrDefault(key, 0.0) + ((data.getPrice() * data.getQuantity()) / 10.0));
                        groupCalculationsTwo.put(key, groupCalculationsTwo.getOrDefault(key, 0.0) + (data.getQuantity() / 10.0));
                        groupCalculations.put(key, groupCalculationsOne.getOrDefault(key, 0.0) / groupCalculationsTwo.getOrDefault(key, 0.0));
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                    for (Map.Entry<String, Double> entry : groupCalculations.entrySet()) {
                        String grup = entry.getKey();

                        String contract = grup;  // PH24091006
                        String year = "20" + contract.substring(2, 4); // "20" + "24" = 2024
                        String month = contract.substring(4, 6); // "09" = Eylül
                        String day = contract.substring(6, 8); // "10" = 10. gün
                        String hour = contract.substring(8, 10); // "06" = 06:00

                        String dateTimeString = day + "/" + month + "/" + year + " " + hour + ":00";
                        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, formatter);

                        String toplamIslemMiktari = String.format("%.2f", groupCalculationsTwo.get(entry.getKey()));
                        String toplamIslemTutari = String.format("%.2f", groupCalculationsOne.get(entry.getKey()));
                        String agirlikOrtFiyat = String.format("%.2f", entry.getValue());

                        model.addRow(new Object[]{grup, dateTime.format(formatter), toplamIslemMiktari, toplamIslemTutari, agirlikOrtFiyat});
                    }
                }
            } else {
                System.out.println("Veri alınamadı. Response Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame.setVisible(true);
    }
}
