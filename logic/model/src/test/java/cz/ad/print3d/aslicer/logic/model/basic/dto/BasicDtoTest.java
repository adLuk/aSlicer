package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;
import cz.ad.print3d.aslicer.logic.model.basic.TemperatureUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BasicDtoTest {

    @Test
    void testTemperatureDto() {
        TemperatureDto dto = new TemperatureDto(new BigDecimal("210.5"), TemperatureUnit.CELSIUS);
        assertEquals(new BigDecimal("210.5"), dto.getValue());
        assertEquals(TemperatureUnit.CELSIUS, dto.getUnit());

        dto.setValue(new BigDecimal("215"));
        assertEquals(new BigDecimal("215"), dto.getValue());
    }

    @Test
    void testDimensionDto() {
        DimensionDto dto = new DimensionDto(new BigDecimal("100"), LengthUnit.MILLIMETER);
        assertEquals(new BigDecimal("100"), dto.getValue());
        assertEquals(LengthUnit.MILLIMETER, dto.getUnits());
    }

    @Test
    void testSpeedDto() {
        SpeedDto dto = new SpeedDto(new BigDecimal("60"), SpeedUnitDto.MILLIMETERS_PER_SECOND);
        assertEquals(new BigDecimal("60"), dto.getValue());
        assertEquals(SpeedUnitDto.MILLIMETERS_PER_SECOND, dto.getUnit());
    }
}
