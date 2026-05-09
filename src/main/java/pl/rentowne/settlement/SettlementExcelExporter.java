package pl.rentowne.settlement;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
class SettlementExcelExporter {

    private static final List<String> HEADERS = List.of(
            "Imię i nazwisko",
            "Kawalerka",
            "Nieruchomość",
            "Umowa aktywna/nieaktywna",
            "Id rozliczenia",
            "Data wygenerowania rozliczenia",
            "Data rozliczenia",
            "Kwota odstępnego",
            "Suma",
            "Kwota czynszu",
            "Kwota za internet",
            "Opłata za gaz",
            "Kwota za prąd",
            "Prąd z licznika",
            "Kwota za wodę",
            "Kwota całkowita",
            "Opis proporcjonalności"
    );

    byte[] export(List<SettlementExportRowDto> settlements) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("settlements");
            CellStyle amountCellStyle = createAmountCellStyle(workbook);
            CellStyle dateCellStyle = createDateCellStyle(workbook);
            CellStyle dateTimeCellStyle = createDateTimeCellStyle(workbook);
            createHeaderRow(sheet);

            for (int i = 0; i < settlements.size(); i++) {
                createDataRow(sheet.createRow(i + 1), settlements.get(i), amountCellStyle, dateCellStyle, dateTimeCellStyle);
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

    private void createDataRow(
            Row row,
            SettlementExportRowDto settlement,
            CellStyle amountCellStyle,
            CellStyle dateCellStyle,
            CellStyle dateTimeCellStyle
    ) {
        setCellValue(row, 0, settlement.tenantFullName());
        setCellValue(row, 1, settlement.rentedObjectName());
        setCellValue(row, 2, settlement.apartmentName());
        setCellValue(row, 3, settlement.contractStatus());
        setCellValue(row, 4, settlement.settlementId());
        setCellValue(row, 5, settlement.generatedDate(), dateTimeCellStyle);
        setCellValue(row, 6, settlement.date(), dateCellStyle);
        setCellValue(row, 7, settlement.compensationAmount(), amountCellStyle);
        setCellValue(row, 8, settlement.rentAndUtilitiesAmount(), amountCellStyle);
        setCellValue(row, 9, settlement.rentAmount(), amountCellStyle);
        setCellValue(row, 10, settlement.internetFee(), amountCellStyle);
        setCellValue(row, 11, settlement.gasDeposit(), amountCellStyle);
        setCellValue(row, 12, settlement.electricityAmount(), amountCellStyle);
        setCellValue(row, 13, settlement.distributedElectricityAmount(), amountCellStyle);
        setCellValue(row, 14, settlement.waterAmount(), amountCellStyle);
        setCellValue(row, 15, settlement.totalAmount(), amountCellStyle);
        setCellValue(row, 16, settlement.prorationDescription());
    }

    private void setCellValue(Row row, int column, Long value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        } else {
            row.createCell(column).setBlank();
        }
    }

    private void setCellValue(Row row, int column, BigDecimal value, CellStyle style) {
        if (value != null) {
            Cell cell = row.createCell(column);
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        } else {
            row.createCell(column).setBlank();
        }
    }

    private void setCellValue(Row row, int column, LocalDateTime value, CellStyle style) {
        if (value != null) {
            Cell cell = row.createCell(column);
            cell.setCellValue(Timestamp.valueOf(value));
            cell.setCellStyle(style);
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

    private CellStyle createAmountCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createDateCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createDateTimeCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
}
