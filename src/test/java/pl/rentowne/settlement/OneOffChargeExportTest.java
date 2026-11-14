package pl.rentowne.settlement;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OneOffChargeExportTest {
    @Test
    void shouldKeepRentAndUtilitiesSeparateAndExportAllChargeComponents() throws Exception {
        Settlement settlement = Settlement.builder().id(1L).settlementDate(LocalDateTime.of(2026, 6, 1, 0, 0))
                .compensationAmount(new BigDecimal("2000")).rentAmount(new BigDecimal("300"))
                .internetFee(new BigDecimal("40")).gasDeposit(new BigDecimal("94.79"))
                .depositTopUp(new BigDecimal("500")).additionalCharge(new BigDecimal("75"))
                .additionalChargeDescription("Dorobienie klucza").totalAmount(new BigDecimal("3009.79")).build();
        settlement.addCharge(new SettlementChargeDto(SettlementChargeDto.Type.DEPOSIT, new BigDecimal("500"), "Kaucja"));
        settlement.addCharge(new SettlementChargeDto(SettlementChargeDto.Type.OTHER, new BigDecimal("75"), "Dorobienie klucza"));
        SettlementMapper mapper = new SettlementMapper();
        var dto = mapper.toListDto(settlement);
        assertThat(dto.totalAmount()).isEqualByComparingTo("3009.79");
        assertThat(dto.rentAndUtilitiesAmount()).isEqualByComparingTo("434.79");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(new SettlementExcelExporter().export(List.of(mapper.toExportRowDto(settlement)))))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(434.79);
            assertThat(row.getCell(15).getNumericCellValue()).isEqualTo(3009.79);
            assertThat(row.getCell(17).getNumericCellValue()).isEqualTo(500);
            assertThat(row.getCell(18).getNumericCellValue()).isEqualTo(75);
            assertThat(row.getCell(19).getStringCellValue()).isEqualTo("Dorobienie klucza");
            var positions = workbook.getSheet("Pozycje naliczeń");
            assertThat(positions.getLastRowNum()).isEqualTo(2);
            assertThat(positions.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Kaucja");
            assertThat(positions.getRow(2).getCell(2).getNumericCellValue()).isEqualTo(75);
            assertThat(positions.getRow(2).getCell(3).getStringCellValue()).isEqualTo("Dorobienie klucza");
        }
    }
}
