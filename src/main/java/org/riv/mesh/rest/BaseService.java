package org.riv.mesh.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BaseService {
	
	Gson gson = new GsonBuilder().setPrettyPrinting().create();

}
