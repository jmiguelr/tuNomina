package com.sim.calendario

class Dependencia {

	String nombre;
	
    static constraints = {
		nombre(nullable:false)
    }
	
	public String toString(){
		return nombre;
	}
}
