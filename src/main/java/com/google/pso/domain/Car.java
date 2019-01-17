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

package com.google.pso.domain;


import java.io.Serializable;

@Deprecated
public class Car implements Serializable {
	
	public final static String CAR="car";//needs to be PK ID for a car
	public final static String MAKE="make";
	public final static String MODEL="model";
	public final static String YEARF="yearF";
	public final static String YEART="yearT";
	public final static String PRICE_MIN="priceMin";
	public final static String PRICE_MAX="priceMax";
	public final static String ZIP="zip";

	private String id;
	
	private int year;
	
	private String make;
	
	private String model;
	
	private String trim;
	
	private String pakage; //TODO: make complex type
	
	private String family;
	
	private String generation;

	/**
	 * 
	 */
	private static final long serialVersionUID = -7376354549050153908L;

	public Car (){
		
	}
	
	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getTrim() {
		return trim;
	}

	public void setTrim(String trim) {
		this.trim = trim;
	}

	public String getPakage() {
		return pakage;
	}

	public void setPakage(String pakage) {
		this.pakage = pakage;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public String getGeneration() {
		return generation;
	}

	public void setGeneration(String generation) {
		this.generation = generation;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
