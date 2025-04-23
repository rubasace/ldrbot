package dev.rubasace.linkedin.games.ldrbot.group;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.ZoneId;

@Converter(autoApply = true)
public class TimeZoneAttributeConverter implements AttributeConverter<ZoneId, String> {

    @Override
    public String convertToDatabaseColumn(ZoneId timeZone) {
        return timeZone != null ? timeZone.getId() : null;
    }

    @Override
    public ZoneId convertToEntityAttribute(String dbData) {
        return dbData != null ? ZoneId.of(dbData) : null;
    }
}