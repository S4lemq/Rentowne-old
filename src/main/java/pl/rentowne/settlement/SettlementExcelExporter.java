package pl.rentowne.settlement;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Component
class SettlementExcelExporter {

    private static final List<String> HEADERS = List.of(
            "id",
            "date",
            "compensationAmount",
            "rentAmount",
            "internetFee",
            "gasDeposit",
            "electricityAmount",
            "distributedElectricityAmount",
            "waterAmount",
            "gasAmount",
            "totalAmount",
            "rentAndUtilitiesAmount",
            "prorationDescription"
    );

    byte[] export(List<SettlementListDto> settlements) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("settlements");
            createHeaderRow(sheet);

            for (int i = 0; i < settlements.size(); i++) {
                createDataRow(sheet.createRow(i + 1), settlements.get(i));
            }

            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot generate settlements excel file", e);
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.size(); i++) {
            headerRow.createCell(i).setCellValue(HEADERS.get(i));
        }
    }

    private void createDataRow(Row row, SettlementListDto settlement) {
        setCellValue(row, 0, settlement.id());
        setCellValue(row, 1, settlement.date());
        setCellValue(row, 2, settlement.compensationAmount());
        setCellValue(row, 3, settlement.rentAmount());
        setCellValue(row, 4, settlement.internetFee());
        setCellValue(row, 5, settlement.gasDeposit());
        setCellValue(row, 6, settlement.electricityAmount());
        setCellValue(row, 7, settlement.distributedElectricityAmount());
        setCellValue(row, 8, settlement.waterAmount());
        setCellValue(row, 9, settlement.gasAmount());
        setCellValue(row, 10, settlement.totalAmount());
        setCellValue(row, 11, settlement.rentAndUtilitiesAmount());
        setCellValue(row, 12, settlement.prorationDescription());
    }

    private void setCellValue(Row row, int column, Long value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        } else {
            row.createCell(column).setBlank();
        }
    }

    private void setCellValue(Row row, int column, BigDecimal value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.toPlainString());
        } else {
            row.createCell(column).setBlank();
        }
    }

    private void setCellValue(Row row, int column, String value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        } else {
            row.createCell(column).setBlank();
        }
    }
}
