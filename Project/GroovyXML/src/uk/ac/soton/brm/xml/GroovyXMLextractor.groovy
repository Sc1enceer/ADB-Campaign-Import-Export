//this version can extract all XML directly related to the campaign. A number of tags are not found (see below)


package uk.ac.soton.brm.xml

import groovy.xml.XmlUtil

class GroovyXMLextractor {
	//following tags not found -  InteractionType, InteractionCategory, BooleanAttribute, BannerValidationCodeAttribute,  
	//AttributeCategory, StringAttribute, DatabaseLocation, dateAttribute, NuberAttribute, SelectionAttribute, LifeCycleModel
	//CRMEntity, FieldSet, CommunicationField, InteractionCategory, InteractionType, ExpressionTag, populationTag, Tag
	//LifecycleTrack, RuleConstant

	//the campaign to look for
	//def static final campaignName = "Enrolment_Chaser_For_New_Students_201213"
	def static final campaignName = "UOS_Grad_Summer_Ceremony_Chase" //has two different users
	//def static final campaignName = "Simple campaign to new UF and CF students"
	

	//get the root node
	def static root = new XmlParser().parse(new File('exp_full.xml')) //the XML file to parse

    static void main(String[] args) {

		//find the campaign
		def campaignXML = root."urn:Campaign".find { it."urn:Name".text() == campaignName }

		//find the admin role XML
		def adminRoleName = campaignXML."urn:AdministratorRole"."urn:role-named-ref".text()
		def adminRoleXML = root."urn:Role".find { it."urn:Name".text() == adminRoleName }

		//there could be multiple users associated with the campaign
		Set allUsers = []

		//find the user and roleassgnment for the campaign 'owner'
		def campaignOwner = campaignXML."urn:population-named-ref"."urn:Owner".text()
		allUsers.add(campaignOwner)

		//find the population list XML
		def popListName = campaignXML."urn:population-named-ref"."urn:Name".text()
		def popListXML = root."urn:Population".find { it."urn:Name".text() == popListName }
		def popListOwner = popListXML."urn:Owner".text()
		
		allUsers.add(popListOwner)
								 
		def popListExpressionXML //some pop lists also have an ExecutionContext containing Expressions
		def expressionCreator

		def expressionText

		if(popListXML."urn:ExecutionContext"){ //the pop list has an Execution context so need to search for expressions

			def popListExpressionName = popListXML."urn:ExecutionContext"."urn:ExpressionName".text()
			popListExpressionXML = root."urn:Expression".find { it."urn:Name".text() == popListExpressionName }

			//get expression creator details
			expressionCreator = popListExpressionXML."urn:Creator".text()
			
			allUsers.add(expressionCreator)

			//the expression is malformed due to escape characters being inserted, should look like this...
			expressionText = popListExpressionXML."urn:ExpressionDefinition"."urn:XmlExpression".text() //note - expression text not needed
			//need to check if this is still an issue when written to file
			
	//	popListExpressionXML."urn:ExpressionDefinition"."urn:XmlExpression".replaceNode ( { "urn:XmlExpression" }) 
println expressionText
		//	popListExpressionXML."urn:ExpressionDefinition"."urn:XmlExpression"[0].value = ["<![CDATA[ <?xml version=\" encoding=\"UTF-8\" standalone=\"yes\"?><DataSource>literal</DataSource><Value>S%</Value><Property><Name>groupId</Name>"] //doesnt work, try removing the node and adding a new node with correct text
			// "<![CDATA[ x ]]>"
popListExpressionXML."urn:ExpressionDefinition"."urn:XmlExpression"[0].value = ["<![CDATA[" +expressionText+"]]>"]
		}
	
		
		// //find communication template info, begin by getting a list of all template names
		def templateLocation = campaignXML."urn:CampaignModel"."urn:Nodes"."urn:MCCActivity"."urn:communication-template-named-ref"
		//get distinct list of template and folder names in case there are duplicates
		//Set templateNames = templateLocation."urn:TemplateName".collect() { it.text() }
		Set templateNames = templateLocation."urn:TemplateName"*.text() //also works
		Set folderNames = templateLocation."urn:folder-named-ref".collect() { it.text() }
		

		def objectsNode = new Node(null, 'objects') //the output, with a root node of 'objects'

		//get watchlists associated with the poplist
		root."urn:WatchList".each { //there must be a groovier way of doing this, probably using a findall{} but....
									if(it."urn:WatchedPopulation"."urn:PopulationHeader"."urn:PopulationName".text() == popListName){
									//	objectsNode.append(it) //found a matching watchlist, add to output
															
										//add user info
										def watchListCreator = it."urn:WatchedPopulation"."urn:UserID".text()													
										allUsers.add(watchListCreator)
									}
								}
		
	
		
		//iterate over template names to get templateXML as string
		//get communication template and template versions
		templateNames.each { template ->
			//use .each to iterate the list of template names without returning a collection
			def commTemplateXML = root."urn:CommunicationTemplate".find { it."urn:Name".text() == template }
			
			//maybe get the communication template owner if one exists
			if (commTemplateXML."urn:Owner"){
				def commTempOwner = commTemplateXML."urn:Owner".text()
				allUsers.add(commTempOwner)
			}
			
			
		//	objectsNode.append(commTemplateXML) // do this later on instead
		}
		
		//find folders assosciated with comm template
		folderNames.each { folder ->
			def folderXML = root."urn:Folder".find { it."urn:Name".text() == folder }
			//append to output node
		//	objectsNode.append(folderXML)
		}

		//find templateversions assosciated with comm template
		templateNames.each { template ->
			def templateVerXML = root."urn:CommunicationTemplateVersion".find { it."urn:template-named-ref"."urn:template-named-ref".text() == template}
			//append to output node
		//	objectsNode.append(templateVerXML)
		}
		
		//Find rules
		//Rules are associated with a campaign --> urirulesetmap --> ruleset --> Rule -->
		//relationship is: campaign <-1-M-> urirulesetmap <-1-1-> ruleset <-1-M-> Rule
		
		//set of rule uri's used in a campaign
		Set ruleURInames = campaignXML."urn:CampaignModel"."urn:Nodes"."urn:RuleNode".collect {
			def ruleURI = it."urn:RuleUri".text()
		}
		
		Set testURI = []
		//some ruleURI's are not linked to a campaign but have a SystemReqTag = true. I'm assuming these are required, by the system
		root."urn:UriRuleSetMap".each { 
										if(it."urn:SystemReqInd".text() == "true"){
											def reqURI = it."urn:Uri".text()
											ruleURInames.add(reqURI)
										}
									  }
		
		Set ruleSets = []
		Set rules = []
		Set ruleConstants = [] 
		
		//get the urirulesetmaps for each rule uri
		ruleURInames.each { ruleURI ->
							def uriRuleSetMapXML = root."urn:UriRuleSetMap".find {  it."urn:Uri".text() == ruleURI 
																					def ruleSetName = it."urn:RuleSetName".text()
																					ruleSets.add(ruleSetName)
																				 }	
						//	objectsNode.append(uriRuleSetMapXML)
						  }
		
		
		//get the RuleSets for each UriRuleSetMap
		ruleSets.each { ruleSetName -> 
						def ruleSetXML = root."urn:RuleSet".find { 	ruleSet ->
																	ruleSet."urn:Name".text() == ruleSetName												
																 }
					//	objectsNode.append(ruleSetXML)
						
						//extract the rule names (there can be more than one rule per ruleset)
						ruleSetXML."urn:RuleHolder".each{ ruleHolder ->
														  def ruleName = ruleHolder."urn:RuleName".text()
														  rules.add(ruleName)
														}
					  }
		
		//get the ruleXML for each rule name found
		rules.each {ruleName ->
					def ruleXML = root."urn:Rule".find { it."urn:Name".text() == ruleName }
					objectsNode.append(ruleXML)
					}
		
		//TODO each Rule contains many RuleArguments. Each RuleArgument CAN have a RuleConstantName which links the argument to a RuleConstant tag

// begin remove this bit
		//combine the XML nodes in to one
//		String outputXML = "<objects>\n"

		//these will need to be reordered in to the order they appear in the original file
		//outputXML = addXML(addXML(addXML(addXML(addXML(outputXML,adminRoleXML),campaignOwnerXML),campaignRoleAssignmentXML),campaignOwnerRoleXML),popListExpressionXML) + "</objects>"
//		outputXML = addXML(outputXML,adminRoleXML)
		//remove definitions added by XmlUtil.serialialize
//		outputXML = outputXML.replaceAll(/<\?xml version="1.0" encoding="UTF-8"\?>/, "")
//		outputXML = outputXML.replaceAll("(?m)^[ \t]*\r?\n", ""); //remove the empty line where the def used to be
// end remove
		
		//println folderXMLs
		
	//	def usersAndAssosciatedXML = getUserAndAssosciatedXML(allUsers)
		
	//	objectsNode.append(campaignXML)
	//	objectsNode = getUserAndAssosciatedXML(allUsers, objectsNode)
		
		println XmlUtil.serialize(popListExpressionXML)

	}
	
	def static getUserAndAssosciatedXML(Set allUsers, Node node){
		//for each user, need to find the userXML, roleXML, roleAssignemtXML
		//related to roles are Organizations, organisations have a OrganizationAddressConfig
		//an organisation may have a parent identifier, if they do not they are the root parent 
		
		Set roles = [] //need somewhere to store the roles which are found in roleassignments
		Set orgs = [] //to store the organizations
		Set orgsAndParents = [] //some orgs have parents, this set is used to create an absolute set of orgs to search for
		Set mailboxes = [] // each organisation has an organization address config, which each has a mailbox account
		
		def usersXML = allUsers.each { //for each user get the userXML
										def userXML = getUserXML(it)
										node.append(userXML)
									 }
		
		def roleAssignmentsXML = allUsers.each { //for each user get the role assignment XML
													def roleAssignmentXML = getRoleAssignmentXML(it)
													roles.add(roleAssignmentXML."urn:RoleName".text())
													
													//each roleassignment has an organisation
													orgs.add(roleAssignmentXML."urn:OrganizationName".text())
														
													node.append(roleAssignmentXML)
											 	}
		
		def rolesXML = roles.each { //for each role get the roleXML
									def roleXML = getUserRoleXML(it)
									node.append(roleXML)
								  }
		
		while(orgs != orgsAndParents){ //in order to deal with nested organisations, ie - parent organisations with more parents,
									   //keep looking for organizations until they are all found
			orgsAndParents = getAllOrganizations(orgs)
			def orgsAndParents2 = getAllOrganizations(orgsAndParents)
			
			if(orgsAndParents == orgsAndParents2){
				orgs = orgsAndParents
			}else{
				orgs = orgsAndParents2
			}
		}

		def orgsXML = orgs.each { //for each roleassignment get the organization XML
									def orgXML = getOrganizationXML(it)
									node.append(orgXML)
								}
		
		def addressConfigsXML = orgs.each { //for each organization get the organizationAddressConfig, if there is one
											def addrConfigXML = getAddressConfigXML(it)
												if(addrConfigXML){
													node.append(addrConfigXML)
													mailboxes.add(addrConfigXML."urn:EmailConfig"."urn:sender-named-ref".text())
												}
										  }
		
		def mailboxesXML = mailboxes.each { //for each organizationAddressConfig get the MailboxAccount
											def mailboxXML = getMailboxAccountXML(it)
											node.append(mailboxXML)
									   	}
		
		return node
	}
	
	def static getUserXML(String user){ //users are referenced in many places so need a method in case multiple user details are required
		//get the user xml
		Node userXML = root."urn:User".find { it."urn:UserId".text() == user }
	}

	def static getRoleAssignmentXML(String user){
		//get the RoleAssignemt for the user
		def userRoleAssignment = root."urn:RoleAssignment".find { it."urn:UserId".text() == user }
	}

	def static getUserRoleXML(String role){
		//get the RoleAssignemt for the user
		def userRole = root."urn:Role".find { it."urn:Name".text() == role }
	}
	
	def static getOrganizationXML(String org){
		//get the Organization for a roleassignment
		def organization = root."urn:Organization".find { it."urn:Name".text() == org }
	}
	
	def static getAddressConfigXML(String org){
		//get the Organization address config for an Organization
		def addrConfig = root."urn:OrganizationAddressConfig".find { it."urn:OrganizationName".text() == org }
	}
	
	def static getMailboxAccountXML(String mailbox){
		//get the Organization address config for an Organization
		def mailboxAccount = root."urn:MailboxAccount".find { it."urn:Name".text() == mailbox }
	}
	
	def static getAllOrganizations(Set orgs){
		//find all parent orgs assosciated with orgs
		Set moreOrgs = []
		
		def allOrgs = orgs.each { //for each roleassignment get the organization XML
									def orgXML = getOrganizationXML(it)
									moreOrgs.add(it)
									//org might have a parent
									if(orgXML."urn:ParentIdentifier"){
										def orgParent = orgXML."urn:ParentIdentifier".text()										
										moreOrgs.add(orgParent)
									}
								}
		return moreOrgs
	}

/*
	def static String addXML(String s, Node node){
		String appendThis = XmlUtil.serialize(node).toString()
		String appended = s + appendThis
	}
*/
}
