import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;


public class SimpleContacts {
	private static final String LINE_SEPARATOR = "------------------";
	private static final String CONTACT_DOMAIN_TITLE = "MySimpleContacts";
	private static final String FIRST_KEY = "First";
	private static final String LAST_KEY = "Last";
	private static final String PHONE_KEY = "Phone";
	private static final String EMAIL_KEY = "Email";
	private static final String STREET_KEY = "Street";
	private static final String CITY_KEY = "City";
	private static final String STATE_KEY = "State";
	private static final String ZIP_KEY = "Zip";
	private static final String TAG_KEY = "Tag";
	private static final String BIRTHDAY_KEY = "Birthday";
	
	private static Scanner scn = new Scanner(System.in);
	private static AmazonSimpleDB simpleDBClient;
	private static String selectedContactId;
	private static Random random = new Random();
	
	public static void main(String[] args) {
		//welcome the user and give them a chance to edit environment variables before continuing
		System.out.println("Welcome to the Simple Contact Manager");
		System.out.println(LINE_SEPARATOR);
		System.out.println("***Make sure you have edited your environment variables to include your AWS access keys before continuing***");
		System.out.println(LINE_SEPARATOR);
		System.out.println("Press enter to continue...");
		
		//wait for enter before proceeding
		scn.nextLine();
		
		//get a SimpleDBClient
		simpleDBClient = getSimpleDBClient();
		
		//ensure that the MySimpleContacts domain exists for this user
		ensureDomainExists();
        
		//formatting
		System.out.println(LINE_SEPARATOR);
		
		while (true) {
			System.out.println("\nPlease select an option below by entering the corresponding number and pressing enter\n");
			System.out.println("0 Exit the program");
			System.out.println("1 List contacts");
			System.out.println("2 Select contact");
			System.out.println("3 Retrieve details about selected contact");
			System.out.println("4 Edit details about selected contact");
			System.out.println("5 Create new contact");
			System.out.println("6 Search contacts");
			
			//call the operation corresponding to the user's choice
			handleUserChoice(scn.nextLine());
		}

	}
	
	/********************************************************************
	* Get SimpleDB client using the user's credentials
	*********************************************************************/
	private static AmazonSimpleDB getSimpleDBClient() {
		AWSCredentials myCredentials;
		AmazonSimpleDB simpleDBClient = null;
		
		try {
			//get credentials from environment variables
			//myCredentials = new EnvironmentVariableCredentialsProvider().getCredentials();
			myCredentials = new BasicAWSCredentials("", "");
			simpleDBClient = new AmazonSimpleDBClient(myCredentials); 
		} catch (Exception ex) {
			System.out.println("There was a problem reading your credentials.");
			System.out.println("Please make sure you have updated your environment variables with your AWS credentials and restart.");
			System.exit(0);
		}
		
		return simpleDBClient;
	}

	/********************************************************************
	* Make sure the contacts domain exists for our user on SimpleDB.
	* If not, create it.
	*********************************************************************/
	private static void ensureDomainExists() {
		//Although createDomain() is idemponent, the documentation warns that it
		//could take up to 10 seconds to create the domain, so we should only call createDomain() once per user
		boolean domainExists = false;
		ListDomainsResult domains = simpleDBClient.listDomains();
	
		for (String domainName : domains.getDomainNames()) {
			if (domainName.equals(CONTACT_DOMAIN_TITLE)) {
				domainExists = true;
				break;
			}
		}
		
		if (!domainExists) {
			try {
				simpleDBClient.createDomain(new CreateDomainRequest(CONTACT_DOMAIN_TITLE));
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
	}

	/********************************************************************
	* Handle user's input for contact manipulation options
	*********************************************************************/
	private static void handleUserChoice(String userInput) {
		//initialize choice to an invalid option
		int choice = -1;
		
		try {
			//get the user's choice from the console
			choice = Integer.parseInt(userInput);
		} catch (NumberFormatException ex) {
			System.out.println(userInput + " is not a valid option. Please enter one of the numbers given");
		}
		
		//handle the user's choice
		switch(choice) {
		case 0:
			//terminate the program
			System.out.println("\nThank you for using Simple Contact Manager. Goodbye.");
			System.exit(0);
			break;
		case 1:
			//list all contacts
			listContacts();
			break;
		case 2:
			//select a contact
			selectContact();
			break;
		case 3:
			//retrieve details about selected contact
			retrieveContactDetails();
			break;
		case 4:
			//edit details about a selected contact
			editContactDetails();
			break;
		case 5:
			//create a new contact
			createNewContact();
			break;
		case 6:
			//search contacts
			searchContacts();
			break;
		default:
			System.out.println(choice + " is not a valid option. Please enter one of the numbers given");
		}
		
	}

	/********************************************************************
	* List the contacts in the user's contact database
	*********************************************************************/
	private static void listContacts() {
        // Select all contacts
        String selectExpression = "select * from `" + CONTACT_DOMAIN_TITLE + "`";
        
        List<Item> allContacts = simpleDBClient.select(new SelectRequest(selectExpression)).getItems();
        
        if (allContacts.size() > 0) {
        	System.out.println("All contacts:\n");
        	displayContacts(allContacts);
        } else {
        	System.out.println("No contacts have been added yet\n");
        }
	}
	

	/********************************************************************
	* Print a collection of contacts
	*********************************************************************/
	private static void displayContacts(List<Item> contacts) {
        String first = "",
        		last = "";
        
        for (Item item : contacts) {
            System.out.println("Contact ID: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
            	if (attribute.getName().equals(FIRST_KEY)) {
            		first = attribute.getValue();
            	} else if (attribute.getName().equals(LAST_KEY)) {
            		last = attribute.getValue();
            	}
            }
            // print the contact's name
        	System.out.println("Name: " + first + " " + last + "\n");
        	
        	// reset first and last
        	first = "";
        	last = "";
        }
		
	}

	/********************************************************************
	* Let the user select a contact
	*********************************************************************/
	private static void selectContact() {
		System.out.println("Please enter the Contact ID of the contact you would like to select:");
		selectedContactId = scn.nextLine();
		//TODO: verify this is an existing contact
	}
	
	/********************************************************************
	* Retrieve contact info for the selected contact
	*********************************************************************/
	private static void retrieveContactDetails() {
		// check that a contact has been selected
		if (selectedContactId == null) {
			System.out.println("Please select a contact first using this option");
			return;
		}
		
		// build query
        String selectExpression = "select * from `" + CONTACT_DOMAIN_TITLE + "` where itemName() = '" + selectedContactId + "'";
        
        // execute query
        for (Item item : simpleDBClient.select(new SelectRequest(selectExpression)).getItems()) {
            System.out.println("Contact ID: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
            	System.out.println(attribute.getName() + ": " + attribute.getValue());
            }
            System.out.println();
        }
	}
	
	/********************************************************************
	* Let the user edit a contact's information
	*********************************************************************/
	private static void editContactDetails() {
		// check that a contact has been selected
		if (selectedContactId == null) {
			System.out.println("Please select a contact first using option 2");
			return;
		}
		
		//collection of all possible attributes. used to let user add values for keys that this contact does not have
		List<String> unusedAttributes = new ArrayList<String>(Arrays.asList(FIRST_KEY, LAST_KEY, PHONE_KEY, 
				EMAIL_KEY, STREET_KEY, CITY_KEY, STATE_KEY, ZIP_KEY, TAG_KEY, BIRTHDAY_KEY));
		
		Collection<ReplaceableAttribute> updateAttributes = new ArrayList<ReplaceableAttribute>();
		Collection<Attribute> deleteAttributes = new ArrayList<Attribute>();
		
		System.out.println("\nEdit/Delete contact information for contact " + selectedContactId + ":\n");

		// build query
        String selectExpression = "select * from `" + CONTACT_DOMAIN_TITLE + "` where itemName() = '" + selectedContactId + "'";
        
        int modifyOption = -1;
        String newValue = "";
        
        // execute query
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        
        System.out.println("Step 1: Review/Edit/Delete existing attributes\n");
        for (Item item : simpleDBClient.select(selectRequest).getItems()) {
            for (Attribute attribute : item.getAttributes()) {
            	unusedAttributes.remove(attribute.getName());
            	while (modifyOption == -1) {
	            	System.out.println(attribute.getName() + ": " + attribute.getValue());
	            	System.out.println("Enter 0 to skip, 1 to edit, or 2 to delete this attribute");
	            	
	            	try {
	            		modifyOption = Integer.valueOf(scn.nextLine());
	            	} catch (NumberFormatException ex) {
	            		System.out.println("Invalid entry. Please enter 0, 1, or 2");
	            	}
            	}
            	
            	//handle the user's choice
        		switch(modifyOption) {
        		case 0:
        			//skip this attribute
        			break;
        		case 1:
        			//modify this attribute
        			System.out.println("Please enter a new value for this attribute:");
        			
        			if (attribute.getName().equals(TAG_KEY)) {
        				System.out.println("List Tags surrounded by [ ], eg, [cool][smart][awesome]");
        			} else if (attribute.getName().equals(BIRTHDAY_KEY)) {
        				System.out.println("Birthday must be in YYYY-MM-DD format");
        			}
        			newValue = scn.nextLine();
        			
        			//add the new value
        			updateAttributes.add(new ReplaceableAttribute(attribute.getName(), newValue, true));
        			
        			//delete the old value
        			deleteAttributes.add(attribute);
        			break;
        		case 2:
        			//delete this attribute
        			deleteAttributes.add(attribute);
        			
        			//put this attribute name back in unused attributes if it's not currently there
        			if (!unusedAttributes.contains(attribute.getName())) unusedAttributes.add(attribute.getName());
        			
        			break;
        		default:
        			System.out.println(modifyOption + " is not a valid option. Please enter one of the numbers given");
        		}
            	
        		//reset modifyOption
        		modifyOption = -1;
            }
            System.out.println();
        }
        
        System.out.println("Step 2: Input values for unused attributes (or skip optional attributes)\n");
        
        for (String attributeName : unusedAttributes) {
        	System.out.println("Press enter to skip or input a value for " + attributeName);
        	newValue = scn.nextLine();
        	
        	//add the new attribute if one was entered
        	if (newValue.length() > 0) {
        		updateAttributes.add(new ReplaceableAttribute(attributeName, newValue, true));
        	} else if (attributeName.equals(FIRST_KEY)) {
        		while (newValue.length() == 0) {
        			System.out.println("First name is required. Please enter a value for first name:");
        			newValue = scn.nextLine();
        		}
        		//add the new first name value
    			updateAttributes.add(new ReplaceableAttribute(FIRST_KEY, newValue, true));
        	} else {
        		//optional attribute, just skip it
        	}
        }
		
        //perform updates
        if (deleteAttributes.size() > 0 || updateAttributes.size() > 0) {
	        try {
	        	System.out.println("Performing updates. Please wait...");
	        	//delete attributes
	            if (deleteAttributes.size() > 0) simpleDBClient.deleteAttributes(new DeleteAttributesRequest().withDomainName(CONTACT_DOMAIN_TITLE).withItemName(selectedContactId).withAttributes(deleteAttributes));
	    		
	    		//add attributes
	    		if (updateAttributes.size() > 0) simpleDBClient.putAttributes(new PutAttributesRequest().withDomainName(CONTACT_DOMAIN_TITLE).withItemName(selectedContactId).withAttributes(updateAttributes));
	    		System.out.println("Success.");
	        } catch (Exception ex) {
	        	System.out.println("There was a problem performing updates. Please review this contact's details and try again.");
	        }    
        } else {
        	System.out.println("No changes to be made.");
        }
        
	}
	
	/********************************************************************
	* Create a new contact in SimpleDB/S3
	*********************************************************************/
	private static void createNewContact() {
		String first = "",
				last = "",
				phoneNumber = "",
				emailAddress = "",
				streetAddress = "",
				city = "",
				state = "",
				zip = "",
				tags = "",
				birthday = "";
		
		//lists of phone numbers/labels and email addresses/labels
		List<String> phoneRecords = new ArrayList<String>(),
			emailRecords = new ArrayList<String>();
			
		//get the contact's first name
		do {
			System.out.println("Enter the contact's first name (mandatory):");
			first = scn.nextLine();
		} while (first.length() == 0);
		
		//get the contact's last name
		System.out.println("Enter the contact's last name (optional - just press enter to skip):");
		last = scn.nextLine();
		
		//collect phone numbers/labels
		do {
			//reset variable
			phoneNumber = "";
			
			System.out.print("Enter");
			if (phoneRecords.size() == 0) {
				System.out.print(" a ");
			} else {
				System.out.print(" another ");
			}
			System.out.println("phone number for this contact with a label (eg. 773-202-5862, Work)");
			System.out.println("(phone numbers are optional - just press enter to skip):");
			
			phoneNumber = scn.nextLine();
			
			if (phoneNumber.length() > 0) phoneRecords.add(phoneNumber);
			
		} while (phoneNumber.length() > 0);
		
		//collect email addresses/labels
		do {
			//reset variables
			emailAddress = "";
			
			System.out.print("Enter");
			if (emailRecords.size() == 0) {
				System.out.print(" an ");
			} else {
				System.out.print(" another ");
			}
			System.out.println("email address for this contact with a label (eg. samuelh@henrycorp.com, Work)");
			System.out.println("(email addresses are optional - just press enter to skip):");
			emailAddress = scn.nextLine();
			
			if (emailAddress.length() > 0) emailRecords.add(emailAddress);
			
		} while (emailAddress.length() > 0);
		
		//get contact's mailing address
		System.out.println("Enter the street address for this contact (optional - just press enter to skip):");
		streetAddress = scn.nextLine();
		
		//get contact's city
		System.out.println("Enter the city for this contact (optional - just press enter to skip):");
		city = scn.nextLine();
		
		//get contact's city
		System.out.println("Enter the state for this contact (optional - just press enter to skip):");
		state = scn.nextLine();
		
		//get contact's zip code
		System.out.println("Enter the 5 digit zip code for this contact (optional - just press enter to skip):");
		zip = scn.nextLine();
		//TODO: validate 5 digits
		
		//get contact's birthday
		System.out.println("Enter the birthday (must be in YYYY-MM-DD format) for this contact (optional - just press enter to skip):");
		birthday = scn.nextLine();

		System.out.println("Enter tag(s) for this user separated by [ ], eg, [cool][smart][awesome] (optional - just press enter to skip):");
		tags = scn.nextLine();
		

		//create the contact's database record
		if (createContactRecordInSimpleDB(first, last, phoneRecords, emailRecords, streetAddress, city, state, zip, tags, birthday)) {
			//create the contact's S3 page if the record was created correctly
			createContactPageInS3(first, last, phoneRecords, emailRecords, streetAddress, city, state, zip, tags, birthday);
		}
	}
	
	/********************************************************************
	* Create a contact's page in S3
	*********************************************************************/
	private static void createContactPageInS3(String first, String last,
			List<String> phoneRecords, List<String> emailRecords,
			String streetAddress, String city, String state, String zip,
			String tags, String birthday) {
		// TODO Auto-generated method stub
		
	}

	/********************************************************************
	* Create a contact's SimpleDB record
	*********************************************************************/
	private static boolean createContactRecordInSimpleDB(String first,
			String last, List<String> phoneRecords,
			List<String> emailRecords, String streetAddress, String city,
			String state, String zip, String tags, String birthday) {
		
		if (first.length() == 0) {
			System.out.println("There was a problem creating this contact. First name is required. Please try again.");
			return false;
		}
		
		Collection<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>();
		
		//add first name attribute
		attributes.add(new ReplaceableAttribute(FIRST_KEY, first, true));
		
		if (last.length() > 0)  attributes.add(new ReplaceableAttribute(LAST_KEY, last, true)) ;
		
		for (String phoneRecord : phoneRecords) {
			attributes.add(new ReplaceableAttribute(PHONE_KEY, phoneRecord, true));
		}
		
		for (String emailRecord : emailRecords) {
			attributes.add(new ReplaceableAttribute(EMAIL_KEY, emailRecord, true));
		}
		
		//add street address attribute if it was entered
		if (streetAddress.length() > 0) attributes.add(new ReplaceableAttribute(STREET_KEY, streetAddress, true));
		
		//add city attribute if it was entered
		if (city.length() > 0) attributes.add(new ReplaceableAttribute(CITY_KEY, city, true));
		
		//add zip attribute if it was entered
		if (zip.length() > 0) attributes.add(new ReplaceableAttribute(ZIP_KEY, zip, true));
		
		//add state attribute if it was entered
		if (state.length() > 0) attributes.add(new ReplaceableAttribute(STATE_KEY, state, true));
		
		//add tag attributes that were entered
		if (tags.length() > 0) attributes.add(new ReplaceableAttribute(TAG_KEY, tags, true));
		
		//add birthday attribute if it was entered
		if (birthday.length() > 0) attributes.add(new ReplaceableAttribute(BIRTHDAY_KEY, birthday, true));
		
		try {
			simpleDBClient.putAttributes(new PutAttributesRequest().withItemName(String.valueOf(random.nextInt(10000))).withAttributes(attributes).withDomainName(CONTACT_DOMAIN_TITLE));
			System.out.println("Successfully created new contact: " + first + " " + last);
			return true;
		} catch (Exception ex) {
			System.out.println("There was a problem adding your contact to SimpleDB, please try again.");
			System.out.println(ex.getMessage());
			return false;
		}

	}

	/********************************************************************
	* Let the user search for a contact
	*********************************************************************/
	private static void searchContacts() {
		/*
		First name starts with <characters>
		Last name starts with <characters>
		State = <two-character state code>
		zip = <five-digit zip code>
		tag = <a specific value> or <all of a set of delimited values - e.g., friend|co-worker finds everyone who is both a friend and a coworker>
		birthday before or after a specific date, or between two dates.
		These are separate searches. You do not need to combine them (e.g., friends in IL)
		*/
		System.out.println("Enter the number of a search to run");
		System.out.println("1 - First name starts with");
		System.out.println("2 - Last name starts with");
		System.out.println("3 - State equals");
		System.out.println("4 - Zip equals");
		System.out.println("5 - Has Tag");
		System.out.println("6 - Has multiple Tags");
		System.out.println("7 - Birthday before");
		System.out.println("8 - Birthday between");
		System.out.println("9 - Birthday after");
		
		//initialize choice to an invalid option
		int choice = -1;
		
		//get the user's input choice
		try {
			choice = Integer.valueOf(scn.nextLine());
		} catch (NumberFormatException ex) {
			System.out.println(choice + " is not a valid option. Please try again and enter one of the numbers given");
			return;
		}
		
		// build query
        String baseSelectExpression = "select * from `" + CONTACT_DOMAIN_TITLE + "` where ";
        String userInputWhereClause = "";
        String userInputParameter = "";
        
		//handle the user's choice
		switch(choice) {
		case 1:
			//search for First name starts with
			System.out.println("Please enter the character(s) the first name should start with:");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " First like '" + userInputParameter + "%'";
			break;
		case 2:
			//search for Last name starts with
			System.out.println("Please enter the character(s) the last name should start with:");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " Last like '" + userInputParameter + "%'";
			break;
		case 3:
			//State equals
			System.out.println("Please enter the two character state abbreviation (e.g., MD, NY, WA, etc.):");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " State = '" + userInputParameter + "'";
			break;
		case 4:
			//Zip equals
			System.out.println("Please enter the five digit zip code:");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " Zip = '" + userInputParameter + "'";
			editContactDetails();
			break;
		case 5:
			//Has Tag
			System.out.println("Please enter a Tag (just the name, not the brackets):");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " Tag like '[" + userInputParameter + "]'";
			break;
		case 6:
			//Has multiple Tags
			String aTag = "";
			String userInputBeginWhereClause = " Tag like '%[";
			String userInputEndWhereClause = "]%'";
			boolean multipleTagInputFlag = false;
			do {
				if (aTag.length() == 0) {
					System.out.print("Please enter a tag:");
				} else {
					System.out.println("Please enter another tag (or just press enter to finish entering tags):");
					multipleTagInputFlag = true;
				}
				
				aTag = scn.nextLine();
				
				if (aTag.length() > 0) {
					if (multipleTagInputFlag) {
						userInputWhereClause = userInputWhereClause + " and ";
					}
				
					userInputWhereClause = userInputWhereClause + userInputBeginWhereClause + aTag + userInputEndWhereClause;
				}
				
			} while(aTag.length() > 0);
			break;
		case 7:
			//Birthday before
			System.out.println("Please enter the date (in YYYY-MM-DD format) before which to search");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " Birthday < '" + userInputParameter + "'";
			break;
		case 8:
			//Birthday between
			System.out.println("Please enter the date (in YYYY-MM-DD format) after which to search");
			String firstDate = scn.nextLine();
			System.out.println("Please enter the date (in YYYY-MM-DD format) before which to search");
			String secondDate = scn.nextLine();
			userInputWhereClause = " Birthday > '" + firstDate + "' and Birthday < '" + secondDate + "'";
			break;
		case 9:
			//Birthday after
			System.out.println("Please enter the date (in YYYY-MM-DD format) after which to search");
			userInputParameter = scn.nextLine();
			userInputWhereClause = " Birthday > '" + userInputParameter + "'";
			break;
		default:
			System.out.println(choice + " is not a valid option. Please enter one of the numbers given");
			return;
		} 
		
		List<Item> matchingContacts = simpleDBClient.select(new SelectRequest(baseSelectExpression + userInputWhereClause)).getItems();
		
		if (matchingContacts.size() > 0) {
			System.out.println("\nResults:\n");
			displayContacts(matchingContacts);
		} else {
			System.out.println("No contacts matched your search.");
		}
	}
}

