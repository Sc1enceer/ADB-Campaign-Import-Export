package uk.ac.soton.brm.xml

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

class Questions {

	static void main(String[] args) {
		
		/*
		 * in the follwing method call is there a way to put the closure after the close parenthesis?
		 */
		
		   println XmlUtil.serialize(new StreamingMarkupBuilder().bind {
			 it.objects objects
		   }) //find out how to avoid close parenthesis here
	  
	}
}
