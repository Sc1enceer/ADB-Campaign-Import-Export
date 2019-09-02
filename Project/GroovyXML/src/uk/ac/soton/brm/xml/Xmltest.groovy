package uk.ac.soton.brm.xml

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

class Xmltest{
	
	def static root = new XmlSlurper().parse(new File('test.xml')) //maybe try slurper?

    static void main(String[] args) {
		
		//need to output a node as xml without escape characters
		def objectsNode = new Node(null, 'objects') //the output, with a root node of 'objects'
		//get a node with escaped chars
		def popListExpressionXML = root.Expression.each { //change source file to make this work
			objectsNode.appendNode(it)
	//		println it.text()
		}
		
		//this bit of code can print unescaped chars
		//def comment = "<![CDATA[<!-- address is new to this release -->]]>"
		def builder = new groovy.xml.StreamingMarkupBuilder()
		builder.encoding = "UTF-8"
		def person = {
		
		// person(id:100){
			
			mkp.yieldUnescaped(objectsNode)
		
		 // }
		}
		
	
		
		def writer = new FileWriter("person.xml")
		writer << builder.bind(person)
		
	//	println XmlUtil.serialize(popListExpressionXML)
	/*	
		def streamingMarkupBuilder=new StreamingMarkupBuilder()
		streamingMarkupBuilder.encoding = "UTF-8"
		println XmlUtil.serialize(streamingMarkupBuilder.bind{mkp.yield popListExpressionXML})
		
	*/	
	}
}