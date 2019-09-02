//this version can extract all XML directly related to the campaign. A number of tags are not found (see below)
//last good version

package uk.ac.soton.brm.xml

import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

class GroovyXMLextractor_Std {
	//following tags not found -  InteractionType, InteractionCategory, BooleanAttribute, BannerValidationCodeAttribute,
	//AttributeCategory, StringAttribute, DatabaseLocation, dateAttribute, NumberAttribute, SelectionAttribute, LifecycleModel
	//CRMEntity, FieldSet, CommunicationField, ExpressionTag, populationTag, Tag
	static String termStr = ""; // term variable
	static boolean includeBase = false

//TODO remove root org from exclude base

	//the campaign to look for
	def static final campaignName = "201920_Enrolment_Chase_New_Entrants_V22"
	//def static final campaignName = "UOS_Grad_Summer_Ceremony_Chase" //has two different users
	//def static final campaignName = "Simple campaign to new UF and CF students"


	//get the root node
	def static root = new XmlParser().parse(new File('C:\\Users\\gs2y19\\iSolution\\Groovy\\Karl\\GroovyXML\\DEV2_FULL_20190823.xml')) //the XML file to parse

	static Set orgs = [] //to store the organisations

	static void main(String[] args) {


		/*
		**	create the term value
		*   Alvin Shi
		* 	30/08/19
		 */
		Calendar calendar = Calendar.getInstance()
		termStr = getCurrentTerm(calendar)
		//.out.println(termStr)


		def objectsNode = new Node(null, 'objects') //the output, with a root node of 'objects'

		//find the campaign
		def campaignXML = root."urn:Campaign".find { it."urn:Name"[0].text() == campaignName }

		//find the campaign's admin role XML
		def adminRoleName = campaignXML."urn:AdministratorRole"."urn:role-named-ref".text()
		//for includebase=false... dont want the admin role xml if the admin role is assigned to the Admin user
		def adminRoleXML
		if ((includeBase==false && adminRoleName!="Admin") || includeBase==true){
			adminRoleXML = root."urn:Role".find { it."urn:Name".text() == adminRoleName }
		}

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
			expressionText = popListExpressionXML."urn:ExpressionDefinition"."urn:XmlExpression".text() //note - expressionText not needed in the output, just here to illustrate issue
			//need to check if this is still an issue when written to file
		}


		// //find communication template info, begin by getting a list of all template names
		def templateLocation = campaignXML."urn:CampaignModel"."urn:Nodes"."urn:MCCActivity"."urn:communication-template-named-ref"
		//get distinct list of template and folder names in case there are duplicates
		//Set templateNames = templateLocation."urn:TemplateName".collect() { it.text() }
		Set templateNames = templateLocation."urn:TemplateName"*.text() //also works
		Set folderNames = templateLocation."urn:folder-named-ref".collect() { it.text() }

		//add XML found so far to the output node
		if(campaignXML){objectsNode.append(campaignXML)}
		if(adminRoleXML){objectsNode.append(adminRoleXML)}
		if(popListXML){objectsNode.append(popListXML)}
		if(popListExpressionXML){objectsNode.append(popListExpressionXML)}



		//get watchlists associated with the poplist
		root."urn:WatchList".each { //there must be a groovier way of doing this, probably using a findall{} but....
			if(it."urn:WatchedPopulation"."urn:PopulationHeader"."urn:PopulationName".text() == popListName){

				objectsNode.append(it) //found a matching watchlist, add to output

				//add user info
				def watchListCreator = it."urn:WatchedPopulation"."urn:UserID".text()
				allUsers.add(watchListCreator)
			}
		}



		//iterate over template names to get templateXML as string
		//get communication template and template versions

		/*
         **     added if statement to filter out old templates
         *      Alvin Shi
         *      30/08/19
         */
		templateNames.each { template ->
			//use .each to iterate the list of template names without returning a collection
			System.out.println("Communication Template " + template)
			if (template.toString().indexOf(termStr) != -1){
				def commTemplateXML = root."urn:CommunicationTemplate"
						.find { it."urn:Name".text() == template }

				//maybe get the communication template owner if one exists
				if (commTemplateXML."urn:Owner"){
					def commTempOwner = commTemplateXML."urn:Owner".text()
					allUsers.add(commTempOwner)
				}

				//get the organisation
				def orgName = commTemplateXML."urn:organization-named-ref".text()

				orgs.add(orgName)
				objectsNode.append(commTemplateXML) // do this later on instead
			}




		}

		/*
			Commented out to eliminating unnecessary xml elements for the campaign
			Alvin Shi
			09/30/19
			************************************************************************************
			**			includes userXML, roleXML, roleAssignemtXML							  **
			** 	 folderNames.each { folder ->												  **
			**		if (folder.toString().indexOf(termStr) != -1){							  **
			**		def folderXML = root."urn:Folder".find { it."urn:Name".text() == folder } **
			**		//append to output node													  **
			**		objectsNode.append(folderXML)											  **
			**		}	}																	  **
			************************************************************************************

		 */

		Set commFieldNames = [] // somewhere to hold the names of dynamic fields found in templates

		/*
        **     added if statement to filter out old templates
        *      Alvin Shi
        *      30/08/19
        */
		//find templateversions assosciated with comm template
		templateNames.each { template ->
			System.out.println("Coomunication Template Version : " + template)
			if (template.toString().indexOf(termStr) != -1){
				root."urn:CommunicationTemplateVersion".findAll {
					if(it."urn:template-named-ref"."urn:template-named-ref".text() == template){
						objectsNode.append(it)

						//test1
						//	  println it."urn:CommunicationMethods"."urn:EmailTemplate"."urn:Content"

						//need to find all 'mail merge' fields in order to locate associated rules
						//  String commContent = "::This is a:: test ::here is another:: test"
						String commContent = it."urn:CommunicationMethods"."urn:EmailTemplate"."urn:Content"
						def fieldMatcher = commContent =~ /::.*?::/ //I HATE WORKING OUT REGEXES

						fieldMatcher.each {
							//add all the fields found to the set
							commFieldNames.add(it)
						}
					}
				}
			}

			//append to output node
			//	objectsNode.append(templateVerXML)
			//	println XmlUtil.serialize(objectsNode)

		}


		//	println commFieldNames

		//Find rules
		//Rules are associated with a campaign --> urirulesetmap --> ruleset --> Rule -->
		//relationship is: campaign <-1-M-> urirulesetmap <-1-1-> ruleset <-1-M-> Rule

		//set of rule uri's used in a campaign
		Set ruleURInames = campaignXML."urn:CampaignModel"."urn:Nodes"."urn:RuleNode".collect {
			def ruleURI = it."urn:RuleUri".text()
		}

		Set testURI = []
		//some ruleURI's are not linked to a campaign but have a SystemReqTag = true. I'm assuming these are required, by the system
		//these rules are included in base xml
		if(includeBase == true){
			root."urn:UriRuleSetMap".each {
				if(it."urn:SystemReqInd".text() == "true"){
					def reqURI = it."urn:Uri".text()
					ruleURInames.add(reqURI)
				}
			}
		}


		Set ruleSets = []
		Set rules = []
		Set ruleConstants = []


		/*		get the uriRulesetMaps for each rule uri
         **     added if statement to filter out old Rule URI
         *      Alvin Shi
         *      30/08/19
         */

		ruleURInames.each { ruleURI ->
			//System.out.println("Rule URI : " + ruleURI)
			if (ruleURI.toString().indexOf(termStr) != -1){
				def uriRuleSetMapXML = root."urn:UriRuleSetMap".find {  it."urn:Uri".text() == ruleURI
					//		def ruleSetName = it."urn:RuleSetName".text()
					//		ruleSets.add(ruleSetName)
				}
				def ruleSetName = uriRuleSetMapXML."urn:RuleSetName".text()
				ruleSets.add(ruleSetName)
				objectsNode.append(uriRuleSetMapXML)
			}




			//	println XmlUtil.serialize(objectsNode)
		}


		/*		get the RuleSets for each UriRuleSetMap
         **     added if statement to filter out old Rule URI
         *      Alvin Shi
         *      30/08/19
         */

		ruleSets.each { ruleSetName ->
			if (ruleSetName.toString().indexOf(termStr) != -1) {
				def ruleSetXML = root."urn:RuleSet".find { ruleSet ->
					ruleSet."urn:Name".text() == ruleSetName
				}
				objectsNode.append(ruleSetXML)

				//extract the rule names (there can be more than one rule per ruleset)
				ruleSetXML."urn:RuleHolder".each { ruleHolder ->
					def ruleName = ruleHolder."urn:RuleName".text()
					rules.add(ruleName)
				}
			}
		}


		/*		get the ruleXML for each rule name found
         **     added if statement to filter out old Rule URI
         *      Alvin Shi
         *      30/08/19
         */


		rules.each { ruleName ->
			//System.out.println("Rule Name : " + ruleName)
			if (ruleName.toString().indexOf(termStr) != -1) {
				def ruleXML = root."urn:Rule".find { it."urn:Name".text() == ruleName }
				objectsNode.append(ruleXML)

				//each Rule contains many RuleArguments. Each RuleArgument CAN have a RuleConstantName which links the argument to a RuleConstant tag
				def ruleConstant = ruleXML."urn:RuleArgument"."urn:RuleConstantName".text()

				if (ruleConstant) {
					ruleConstants.add(ruleConstant)
				}
			}
		}


		/*
		**	get ruleConstantXML - but - should I get ruleConstants that are found above,
		* 	or all rule constants. some rule constants dont seem to be associated with a rule
		 */
		//
		ruleConstants.each {ruleConstantName ->
			def ruleConstantXML = root."urn:RuleConstant".find { it."urn:Name".text() == ruleConstantName }
			objectsNode.append(ruleConstantXML)
		}


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



		/*
			Commented out to eliminating unnecessary xml elements for the campaign
			**********************************************************************
			**			includes userXML, roleXML, roleAssignemtXML				**
			** 	 objectsNode = getUserAndAssosciatedXML(allUsers, objectsNode)	**
			**********************************************************************

		 */



		//	println XmlUtil.serialize(commTemplateXML)

		//	def output = XmlUtil.serialize(popListExpressionXML)
/*
		def writer = new FileWriter("campaign1.xml")
		new XmlNodePrinter(new PrintWriter(writer)).print(objectsNode)

		def builder = new MarkupBuilder()

			builder{
			mkp.yieldUnescaped writer.toString() //magically insert a nodelist of arbitrary XML from somewhere else
		}
*/

		//output result to a file
		File outputFile = new File("campaign_201920_Enrolment_Chase_New_Entrants.xml")
		outputFile.write XmlUtil.serialize(objectsNode)



	}


	/*
	** 		Method implemented to find the current term base on the term
	* 		@param1	: Calendar.instance() current date
	* 		checks the month is past august
	* 		Alvin Shi
	* 		30/08/19
	 */
	static String getCurrentTerm(Calendar calendar){
		String termStr = "";
		if (calendar.get(Calendar.MONTH) + 1 >= 8){
			Integer year = calendar.get((Calendar.YEAR));
			Integer nextY = (year + 1) % 1000;
			termStr = Integer.toString(year) + Integer.toString(nextY);
			//System.out.println("new term is : " + termStr);
		} else {
			Integer year = calendar.get((Calendar.YEAR)) - 1;
			Integer nextY = (year + 1) % 1000;
			termStr = Integer.toString(year) + Integer.toString(nextY);
			//System.out.println("new term is : " + termStr);
		}
		return termStr;
	}


	// disabled reason refer to usage
	def static getUserAndAssosciatedXML(Set allUsers, Node node){
		//for each user, need to find the userXML, roleXML, roleAssignemtXML
		//related to roles are Organizations, organisations have a OrganizationAddressConfig
		//an organisation may have a parent identifier, if they do not they are the root parent

		Set roles = [] //need somewhere to store the roles which are found in roleassignments
		//	Set orgs = [] //to store the organizations
		Set orgsAndParents = [] //some orgs have parents, this set is used to create an absolute set of orgs to search for
		Set mailboxes = [] // each organisation has an organization address config, which each has a mailbox account

		//remove Admin user if include base is false
		if(includeBase == false && allUsers.contains("Admin")){
			allUsers.remove("Admin")
		}

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

		//TODO: need to remove the root organisation if base = false
		def orgsXML = orgs.each { //for each roleassignment get the organization XML
			def orgXML = getOrganizationXML(it)
			node.append(orgXML)
		}

		def addressConfigsXML = orgs.each { //for each organization get the organizationAddressConfig, if there is one
			def addrConfigXML = getAddressConfigXML(it)
			if(addrConfigXML){
				node.append(addrConfigXML)
				mailboxes.add(addrConfigXML."urn:EmailConfig"."urn:sender-named-ref".text())
				mailboxes.add(addrConfigXML."urn:EmailConfig"."urn:replyTo-named-ref".text())
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
		//find all parent orgs assosciated with each org
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
