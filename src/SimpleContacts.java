import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;


public class SimpleContacts {
	private static final String LINE_SEPARATOR = "------------------";
	private static final String CONTACT_DOMAIN_TITLE = "MySimpleContacts";
	private static final String FIRST_NAME = "First";
	private static final String LAST_NAME = "Last";
	
	private static Scanner scn = new Scanner(System.in);
	private static AmazonSimpleDB simpleDBClient;
	private static String selectedContactId;
	private static String selectedContactFirst = "";
	private static String selectedContactLast = "";
	
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
        
        //put data into my domain
        //simpleDBClient.batchPutAttributes(new BatchPutAttributesRequest(CONTACT_DOMAIN_TITLE, createSampleData()));
        
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
	
	private static void selectContact() {
		System.out.println("Please enter the Contact ID of the contact you would like to select:");
		selectedContactId = scn.nextLine();
		
	}
	
	private static void retrieveContactDetails() {
		// check that a contact has been selected
		if (selectedContactId == null) {
			System.out.println("Please select a contact first using option 2");
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
	
	private static void editContactDetails() {
		// check that a contact has been selected
		if (selectedContactId == null) {
			System.out.println("Please select a contact first using option 2");
			return;
		}
		
	}
	
	private static void createNewContact() {
		// TODO Auto-generated method stub
		
	}
	
	private static void searchContacts() {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Creates an array of SimpleDB ReplaceableItems populated with sample data.
     *
     * @return An array of sample item data.
     */
    private static List<ReplaceableItem> createSampleData() {
    	
    	List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
    	sampleData.add(new ReplaceableItem("Item_01").withAttributes(
				new ReplaceableAttribute("First", "Samuel", true),
				new ReplaceableAttribute("Last", "Henry", true),
				new ReplaceableAttribute("Tag", "cool", true),
				new ReplaceableAttribute("Tag", "smart", true),
				new ReplaceableAttribute("Birthday", "5/18/1984", true)
				));
    	sampleData.add(new ReplaceableItem("Item_02").withAttributes(
				new ReplaceableAttribute("First", "Cooper", true),
				new ReplaceableAttribute("Last", "Henry", true),
				new ReplaceableAttribute("Tag", "cool", true),
				new ReplaceableAttribute("Tag", "smart", true),
				new ReplaceableAttribute("Tag", "handsome", true),
				new ReplaceableAttribute("Birthday", "6/3/1982", true)
				));
		

        return sampleData;
    }
}

