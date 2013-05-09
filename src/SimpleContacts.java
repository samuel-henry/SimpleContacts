import java.util.ArrayList;
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
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;


public class SimpleContacts {
	private static final String LINE_SEPARATOR = "------------------";
	private static final String CONTACT_DOMAIN_TITLE = "MySimpleContacts";
	private static final String FIRST_NAME = "First";
	private static final String LAST_NAME = "Last";
	
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
		
		//wait for enter
		scn.nextLine();
		
		//get a SimpleDBClient
		simpleDBClient = getSimpleDBClient();
		
		//ensure that the MySimpleContacts domain exists for this user
		ensureDomainExists();
		
		//print domains
        for (String domainName : simpleDBClient.listDomains().getDomainNames()) {
            System.out.println("  " + domainName);
        }
        
		//formatting
		System.out.println(LINE_SEPARATOR);
		System.out.println();
		
		while (true) {
			System.out.println("Please select an option below by entering the corresponding number and pressing enter");
			System.out.println();
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
			System.out.println(userInput + " is invalid. Please enter only the number of the option you want");
		}
		
		//handle the user's choice
		switch(choice) {
		case 0:
			//terminate the program
			System.out.println();
			System.out.println("Thank you for using Simple Contact Manager. Goodbye.");
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
        String first = "",
        		last = "";
        
        System.out.println("All contacts: \n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        for (Item item : simpleDBClient.select(selectRequest).getItems()) {
            System.out.println("Contact ID: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
            	if (attribute.getName().equals(FIRST_NAME)) {
            		first = attribute.getValue();
            	} else if (attribute.getName().equals(LAST_NAME)) {
            		last = attribute.getValue();
            	}
            	
            	
            }
            // print the contact's name
        	System.out.println("Name: " + first + " " + last);
        	
        	// reset first and last
        	first = "";
        	last = "";
        	
        	// print a line break
            System.out.println();
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
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        for (Item item : simpleDBClient.select(selectRequest).getItems()) {
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
		
	}
	
	/********************************************************************
	* Create a new contact in SimpleDB/S3
	*********************************************************************/
	private static void createNewContact() {
		String first = "",
				last = "",
				phoneNumber = "",
				phoneLabel = "",
				emailAddress = "",
				emailLabel = "",
				streetAddress = "",
				city = "",
				state = "",
				zip = "",
				tag = "",
				birthday = "";
		
		//lists of phone numbers/labels and email addresses/labels
		List<String> phoneRecords = new ArrayList<String>(),
			emailRecords = new ArrayList<String>();
		
		//list of tags
		List<String> tags = new ArrayList<String>();
			
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
			//reset variables
			phoneNumber = "";
			phoneLabel = "";
			
			System.out.println("Enter a phone number for this contact with a label (eg. 773-202-5862, Work)");
			System.out.println("(phone numbers are optional - just press enter to skip):");
			phoneNumber = scn.nextLine();
			
			if (phoneNumber.length() > 0) phoneRecords.add(phoneNumber);
			
		} while (phoneNumber.length() > 0);
		
		//collect email addresses/labels
		do {
			//reset variables
			emailAddress = "";
			
			System.out.println("Enter an email address for this contact with a label (eg. samuelh@henrycorp.com, Work)");
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
		System.out.println("Enter the birthday for this contact (optional - just press enter to skip):");
		birthday = scn.nextLine();
		//TODO: separate into month/day/year
		
		do {
			System.out.println("Enter a tag for this user (optional - just press enter to skip):");
			tag = scn.nextLine();
			//TODO: add tags to list and reset variable
		} while (tag.length() > 0);

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
			List<String> tags, String birthday) {
		// TODO Auto-generated method stub
		
	}

	/********************************************************************
	* Create a contact's SimpleDB record
	*********************************************************************/
	private static boolean createContactRecordInSimpleDB(String first,
			String last, List<String> phoneRecords,
			List<String> emailRecords, String streetAddress, String city,
			String state, String zip, List<String> tags, String birthday) {
		
		if (first.length() == 0) {
			System.out.println("There was a problem creating this contact. First name is required. Please try again.");
			return false;
		}
		
		Collection<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>();
		
		//add first name attribute
		attributes.add(new ReplaceableAttribute("First", first, true));
		
		if (last.length() > 0)  attributes.add(new ReplaceableAttribute("Last", last, true)) ;
		
		for (String phoneRecord : phoneRecords) {
			attributes.add(new ReplaceableAttribute("Phone", phoneRecord, true));
		}
		
		for (String emailRecord : emailRecords) {
			attributes.add(new ReplaceableAttribute("Email", emailRecord, true));
		}
		
		//add street address attribute if it was entered
		if (streetAddress.length() > 0) attributes.add(new ReplaceableAttribute("Street", streetAddress, true));
		
		//add city attribute if it was entered
		if (city.length() > 0) attributes.add(new ReplaceableAttribute("City", city, true));
		
		//add zip attribute if it was entered
		if (zip.length() > 0) attributes.add(new ReplaceableAttribute("Zip", zip, true));
		
		//add state attribute if it was entered
		if (state.length() > 0) attributes.add(new ReplaceableAttribute("State", state, true));
		
		//add tag attributes that were entered
		for (String tag : tags) attributes.add(new ReplaceableAttribute("Tag", tag, true));
		
		//add birthday attribute if it was entered
		if (birthday.length() > 0) attributes.add(new ReplaceableAttribute("Birthday", birthday, true));
		
		//List<ReplaceableItem> contacts = new ArrayList<ReplaceableItem>();
		
		try {
			simpleDBClient.putAttributes(new PutAttributesRequest().withItemName(String.valueOf(random.nextInt(10000))).withAttributes(attributes).withDomainName(CONTACT_DOMAIN_TITLE));
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
		// TODO Auto-generated method stub
		
	}
}

