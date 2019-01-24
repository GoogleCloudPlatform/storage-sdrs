package com.google.gcs.sdrs.dao.converter;

import com.google.gcs.sdrs.enums.RetentionRuleType;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static com.google.gcs.sdrs.enums.RetentionRuleType.DATASET;
import static com.google.gcs.sdrs.enums.RetentionRuleType.GLOBAL;

@Converter
public class RetentionRuleTypeConverter implements AttributeConverter<RetentionRuleType, String> {

  /** Convert RetentionRuleType to a String */
  @Override
  public String convertToDatabaseColumn(RetentionRuleType type) {
    return type.toString();
  }

  /** Convert a database string representation to a RetentionRuleType */
  @Override
  public RetentionRuleType convertToEntityAttribute(String databaseRepresentation) {
    switch (databaseRepresentation) {
      case "global":
        return GLOBAL;
      case "dataset":
        return DATASET;
      default:
        throw new IllegalArgumentException(
            String.format(
                "%s is not representable as a %s",
                databaseRepresentation, RetentionRuleType.class));
    }
  }
}
