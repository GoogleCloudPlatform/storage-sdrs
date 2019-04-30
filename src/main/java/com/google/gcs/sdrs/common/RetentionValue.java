/*
 * Copyright 2019 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 * Any software provided by Google hereunder is distributed “AS IS”,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.
 *
 */

package com.google.gcs.sdrs.common;

/** Represent retention value in the form of [number]:[unit] */
public class RetentionValue {
  private Integer number;
  private RetentionUnitType unitType;

  public RetentionValue(Integer number, RetentionUnitType unitType) {
    this.number = number;
    this.unitType = unitType;
  }

  public Integer getNumber() {
    return number;
  }

  public RetentionUnitType getUnitType() {
    return unitType;
  }

  public String getNumberString() {
    return number == null ? null : number.toString();
  }

  public String getUnitTypeString() {
    return unitType == null ? null : unitType.toString();
  }

  public String toDatabaseRepresentation() {

    if (number == null || unitType == null) {
      return null;
    }
    return number.toString() + ":" + unitType.toDatabaseRepresentation();
  }

  /**
   * Parse a retention value string.
   *
   * @param retentionValue  A string in the form of [number]:[unit]
   * @return
   */
  public static RetentionValue parse(String retentionValue) {

    if (retentionValue == null) {
      return null;
    }

    String[] tokens = retentionValue.split(":");
    if (tokens.length == 2) {
      String numberStr = tokens[0];
      String unitTypeStr = tokens[1];
      Integer number = null;
      try {
        number = Integer.parseInt(numberStr);
      } catch (NumberFormatException e) {
        // no-op
      }
      RetentionUnitType unitType = RetentionUnitType.getType(unitTypeStr);
      return new RetentionValue(number, unitType);
    } else {
      return null;
    }
  }

  /**
   * Convert retention value based on unit type
   *
   * @param retentionValue
   * @return
   */
  public static Integer convertValue(RetentionValue retentionValue) {
    if (retentionValue == null
        || retentionValue.getNumber() == null
        || retentionValue.getUnitType() == null) {
      return null;
    }

    if (retentionValue.getUnitType() == RetentionUnitType.MONTH) {
      return retentionValue.getNumber() * 30;
    }
    return retentionValue.getNumber();
  }
}
