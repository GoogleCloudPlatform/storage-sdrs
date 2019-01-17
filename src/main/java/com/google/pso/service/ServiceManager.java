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

package com.google.pso.service;

import java.util.ArrayList;
import java.util.List;

import com.google.pso.domain.Car;

/**
 * Service tier (server side), that feeds data into the Resource tier above it
 * 
 * TODO - add an interface that we'll code against
 * 
 * @deprecated POC code only
 *
 */
@Deprecated
public class ServiceManager {
	
	private static ServiceManager instance;
	//private constructor
	private ServiceManager(){}
	
	public static ServiceManager getInstance ()
	{
		if (instance == null) {
			synchronized (ServiceManager.class) {
	            if(instance == null){
	                instance = new ServiceManager();
	            }
	        }
		}
		return instance;
	}
	

	/**
	 * TODO have it take parameters
	 * @return
	 */
	public List<Car> getCars() {

		List<Car> toReturn = new ArrayList<Car>();

		Car a = new Car();
		a.setMake("Honda");
		a.setModel("Accord");
		a.setYear(1987);
		a.setPakage("DX");

		Car b = new Car();
		b.setMake("Porsche");
		b.setModel("911");
		b.setYear(2004);
		b.setPakage("Turbo");

		Car c = new Car();
		c.setMake("Toyota");
		c.setModel("Celica");
		c.setYear(1982);
		c.setPakage("GT");

		Car d = new Car();
		d.setMake("BMW");
		d.setModel("328");
		d.setYear(1997);
		d.setPakage("Is");

		Car e = new Car();
		e.setMake("Mazda");
		e.setModel("Protege");
		e.setYear(1996);
		e.setPakage("DX");
		
		Car f = new Car();
		f.setMake("Ford");
		f.setModel("Mustang");
		f.setYear(2014);
		f.setPakage("GT");
		
		Car g = new Car();
		g.setMake("Jeep");
		g.setModel("Wrangler");
		g.setYear(2004);
		g.setPakage("Rubicon");

		toReturn.add(a);
		toReturn.add(b);
		toReturn.add(c);
		toReturn.add(d);
		toReturn.add(e);
		toReturn.add(f);
		toReturn.add(g);

		return toReturn;
	}

}
