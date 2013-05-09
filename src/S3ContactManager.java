

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Implementation of basic S3 actions:
 * 		Get AWS credentials
 * 		Create a bucket
 * 		List a bucket's contents
 * 		Create an object
 * 		Edit an object
 * 		Delete an object
 */
public class S3ContactManager {
	private static final String LINE_SEPARATOR = "------------------";
	private static final String BUCKET_NAME_VALID_REG_EX = "[a-z\\d.-]";
	private static final String PERIOD_REG_EX = "[.]";
	private static final String DASH_STRING = "-";
	private static final String HTML_TEMPLATE = "<!DOCTYPE html><html><table><tr><th>First Name</th><th>Last Name</th><th>Phone Number</th></tr><tr><td id=\"firstName\"></td><td id=\"lastName\"></td><td id=\"phoneNumber\"></td></tr></body></html>";

	private static AmazonS3 s3client;
	private static Scanner scn = new Scanner(System.in);
	
	public static void main(String[] args) {
		//welcome the user and give them a chance to edit environment variables before continuing
		System.out.println("Welcome to the S3 Contact Manager");
		System.out.println(LINE_SEPARATOR);
		System.out.println("***Make sure you have edited your environment variables to include your AWS access keys before continuing***");
		System.out.println(LINE_SEPARATOR);
		System.out.println("Press enter to continue...");
		
		//wait for enter
		scn.nextLine();
		
		//formatting
		System.out.println(LINE_SEPARATOR);
		System.out.println();
		
		//indicate whether the user can work with a given bucket name
		boolean bucketNameInputOkToProceed = false;
		
		//let user retry entering bucket name until it is already owned by the account or created
		while (!bucketNameInputOkToProceed) {
			//request the name of an S3 bucket to operate on
			System.out.println("To begin, please enter the name of an S3 bucket:");
			String bucketName = scn.nextLine();
			
			//check if it's a valid bucket name before continuing. if not, restart bucket name input loop
			bucketNameInputOkToProceed = validateBucketName(bucketName);
			
			if (bucketNameInputOkToProceed) {
				// let user interact with selected bucket as many times as they want
				while (true) {
					System.out.println("Please select an option below by entering the corresponding number and pressing enter");
					System.out.println();
					System.out.println("0 Exit the program");
					System.out.println("1 List " + bucketName + "'s contents");
					System.out.println("2 Delete an object in " + bucketName);
					System.out.println("3 Create a new object in " + bucketName);
					System.out.println("4 Edit an object in " + bucketName);
					
					//call the operation corresponding to the user's choice
					handleUserChoice(scn.nextLine(), bucketName);
				}
			}
		}
	}

	/********************************************************************
	* Get an S3 client using the user's credentials
	*********************************************************************/
	public static AmazonS3 getS3Client() {
		AWSCredentials myCredentials;
		AmazonS3 s3client = null;
		
		try {
			//get credentials from environment variables
			myCredentials = new EnvironmentVariableCredentialsProvider().getCredentials();
			s3client = new AmazonS3Client(myCredentials); 
		} catch (Exception ex) {
			System.out.println("There was a problem reading your credentials.");
			System.out.println("Please make sure you have updated your environment variables with your AWS credentials and restart.");
			System.exit(0);
		}
		
		return s3client;
	}

	/********************************************************************
	* Check that a bucket name is OK to work with
	* 	1. Check format of string
	* 	2. Validate that the user can access or create this bucket
	*********************************************************************/
	public static boolean validateBucketName(String bucketName) {
		//check format
		if (!validateBucketNameFormat(bucketName)) {
			return false;
		}
		
		//check whether the bucket is usable
		if (!validateBucketNameUsable(bucketName)) {
			return false;
		}
		
		return true;
	}

	/********************************************************************
	* Validate the format of a bucket name
	*********************************************************************/
	private static boolean validateBucketNameFormat(String bucketName) {
		//check valid length
		if (bucketName.length() < 3 || bucketName.length() > 255) {
			System.out.println("ERROR: Bucket name must be between 3 and 255 characters long, inclusive");
			return false;
		}
		
		//get the invalid characters in the bucket name
		String invalidCharsInBucketName = bucketName.replaceAll(BUCKET_NAME_VALID_REG_EX, "");
		
		//check only contains valid characters (lower case letters, numbers, dashes, periods)
		if (invalidCharsInBucketName.length() > 0) {
			System.out.println("ERROR: The following characters in your bucket name are invalid: " + invalidCharsInBucketName);
			return false;
		}
		
		//check that we do not have successive periods
		if (bucketName.indexOf("..") > -1) {
			System.out.println("ERROR: Bucket names may not have successive periods");
			return false;
		}
		
		//get labels by splitting on periods
		String[] bucketNameLabels = bucketName.split(PERIOD_REG_EX);
		
		//variables for iterating over labels
		String currLabel = "";
		String firstCharInLabelAsString = "";
		String lastCharInLabelAsString = "";
		
		//count the number of labels that are all numeric in order to reject IP-formatted bucket names
		int numAllNumericLabels = 0;
		
		//check valid labels
		for (int i = 0; i < bucketNameLabels.length; i++) {
			//get the current label
			currLabel = bucketNameLabels[i];
			
			//get the current label's first and last characters
			firstCharInLabelAsString = currLabel.substring(0,1);
			lastCharInLabelAsString = currLabel.substring(currLabel.length());
			
			//check that label does not begin or end with dash
			if (firstCharInLabelAsString.equals(DASH_STRING) || lastCharInLabelAsString.equals(DASH_STRING)) {
				System.out.println("ERROR: Labels may only begin with lower case letters or numbers - not dashes");
				return false;
				
			}
			
			//see if label is all numeric to check against IP Address format
			try {
				//try parsing the label into an integer
				Integer.parseInt(currLabel);
				
				//no exception thrown, this label is all numeric. increment number of all numeric labels
				numAllNumericLabels++;
			} catch (NumberFormatException ex) {
				//bucket is not IP address formatted, set count of numeric labels to minimum integer value
				numAllNumericLabels = Integer.MIN_VALUE;
			}
		}
		
		//if all labels in bucket are numeric, the bucket is formatted like an IP address, which is not allowed
		if (numAllNumericLabels == bucketNameLabels.length) {
			System.out.println("ERROR: Bucket name may not be formatted like an IP address (e.g. 192.168.5.4)");
			return false;
		}
		
		//bucket name is of a valid format
		return true;
	}
	
	/********************************************************************
	* Check that a bucket name is usable (i.e. either owned by the account 
	* or able to be (and then) created
	*********************************************************************/
	private static boolean validateBucketNameUsable(String bucketName) {
		//get the S3 client
		s3client = getS3Client();
		
		//check if the bucket already exists on S3
		if (s3client.doesBucketExist(bucketName)) {
			//if it does already exist, check if it's owned by the user's account
			if (bucketNameAlreadyOwnedByUs(bucketName)) {
				return true;
			} else {
				//exists but not owned by user's account. user cannot work with this bucket
				return false;
			}
		} else {
			//can create a bucket of this name. create it!
			return createBucket(bucketName);
		}
	}
	
	/********************************************************************
	* Check if our account already owns this bucket. If  we successfully 
	* get the bucket's access control list, we have access to the bucket. 
	* S3 throws an exception if we don't have access to this bucket
	*********************************************************************/
	private static boolean bucketNameAlreadyOwnedByUs(String bucketName) {
		try {
			//get the S3 client
			s3client = getS3Client();
			
			//get the bucket's access control list. throws an exception if user's account 
			//cannot access it, which indicates that another account owns this bucket
			s3client.getBucketAcl(bucketName);
			return true;
		} catch (Exception ex) {
			System.out.println("You don't have access to this bucket. Please operate on another bucket");
			return false;
		}
	}
	
	/********************************************************************
	* Create the specified bucket 
	*********************************************************************/
	private static boolean createBucket(String bucketName) {
		try {
			//get the S3 client
			s3client = getS3Client();
			
			//create the bucket. return false if there is a problem creating this bucket 
			//(e.g. it's been created in the time since we checked)
			s3client.createBucket(bucketName);
			System.out.println("Bucket created.");
			return true;
		} catch (Exception ex) {
			System.out.println("ERROR: There was a problem creating this bucket. Please try again.");
			return false;
		}
	}

	
	/*******************************************************
	 * Handle user's input for bucket options
	 *******************************************************/
	private static void handleUserChoice(String userInput, String bucketName) {
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
			System.out.println("Thank you for using S3 contact manager. Goodbye.");
			System.exit(0);
			break;
		case 1:
			//list the contents of this bucket
			listContentsInBucket(bucketName);
			break;
		case 2:
			//delete an object in this bucket
			deleteObjectInBucket(bucketName);
			break;
		case 3:
			//create a new object in this bucket
			createNewObjectInBucket(bucketName);
			break;
		case 4:
			//edit an object in this bucket
			editObjectInBucket(bucketName);
			break;
		default:
			System.out.println(choice + " is not a valid option. Please enter one of the numbers given");
		}
		
	}
	
	/*******************************************************
	 * List the contents of the specified bucket
	 *******************************************************/
	private static void listContentsInBucket(String bucketName) {
		try {
			//get the S3 client
			s3client = getS3Client();
			
			//get the list of objects in this bucket
			ObjectListing bucketContents = s3client.listObjects(bucketName);
			List<S3ObjectSummary> bucketObjectSummaries = bucketContents.getObjectSummaries();
			
			//formatting
			System.out.println();
			
			//list contents of bucket (or indicate that it's empty)
			if (bucketObjectSummaries.size() == 0) {
				System.out.println(bucketName + " is currently empty.");
			} else {
				System.out.println("Listing contents:");
				for (S3ObjectSummary object : bucketObjectSummaries) {
					System.out.println(object.getKey() + " | " + object.getSize() + " bytes | " + object.getLastModified().toString());
				}
			}
			
			//formatting
			System.out.println();
		} catch (Exception ex) {
			System.out.println("There was a problem listing the contents of " + bucketName);
			System.out.println("Please try again.");
		}
	}

	/*******************************************************
	 * Delete an object in the specified bucket
	 *******************************************************/
	private static void deleteObjectInBucket(String bucketName) {
		//get the name of the object to delete
		System.out.println("Enter the name (key) of the object you want to delete:");
		String objectToDelete = scn.nextLine();
		
		try {
			//attempt to get the object to delete. if an exception is thrown, 
			//an object with this key likely does not exist
			s3client.getObject(bucketName, objectToDelete);
			
			//delete from this bucket
			s3client.deleteObject(bucketName, objectToDelete);
			System.out.println("Success");
		} catch (AmazonS3Exception ex) {
			//exception retrieving object from S3, potentially due to invalid object key
			System.out.println("ERROR: There was a problem deleting " + objectToDelete + " from S3.");
			System.out.println("       Please verify that the object exists and try again.");
			System.out.println();
		}  catch (Exception ex) {
			System.out.println("There was a problem deleting " + objectToDelete + " from " + bucketName);
			System.out.println("Please try again.");
		}
	}

	/*******************************************************
	 * Create a new object in the specified bucket
	 *******************************************************/
	private static void createNewObjectInBucket(String bucketName) {
		System.out.println("Create a new contact document object");
		try {
			//call object creation method
			createObjectInBucket(bucketName);
		} catch (Exception ex) {
			System.out.println("ERROR: There was a problem. Please try again.");
		}
	}

	/*******************************************************
	 * Edit an object in the specified bucket
	 * This is accomplished by deleting the old object and
	 * creating a new object
	 *******************************************************/
	private static void editObjectInBucket(String bucketName) {
		//get the name of the object to edit
		System.out.println("Edit an existing contact document object");
		System.out.println("Enter the name (key) of the object you want to edit:");
		String objectToEditName = scn.nextLine();
		try {
			//establish an S3 client connection
			s3client = getS3Client();
			
			//attempt to get the object to edit. if an exception is thrown, an object with this key likely does not exist
			s3client.getObject(bucketName, objectToEditName);
			
			//delete the old object
			s3client.deleteObject(bucketName, objectToEditName);
			
			//create a new object with new info
			createObjectInBucket(bucketName);
			
		} catch (AmazonS3Exception ex) {
			//exception retrieving object from S3, potentially due to invalid object key
			System.out.println("ERROR: There was a problem retrieving " + objectToEditName + " from S3.");
			System.out.println("       Please verify the file name and try again.");
			System.out.println();
		} catch (Exception ex) {
			System.out.println("ERROR: There was a problem. Please try again.");
		}
	}

	/*******************************************************
	 * Create an object in the specified bucket
	 *******************************************************/
	private static void createObjectInBucket(String bucketName) throws Exception {
		//copy the document template for creating a new contact document
		String newDocument = HTML_TEMPLATE;
		
		//get the contact's first name
		System.out.println("Enter the contact's first name:");
		String firstName = scn.nextLine();
		
		//get the contact's last name
		System.out.println("Enter the contact's last name:");
		String lastName = scn.nextLine();
		
		//get the contact's phone number
		System.out.println("Enter the contact's phone number:");
		String phoneNumber = scn.nextLine();
		
		//create the new document's text using the entered information
		newDocument = newDocument.replace("<td id=\"firstName\">", "<td id=\"firstName\">" + firstName);
		newDocument = newDocument.replace("<td id=\"lastName\">", "<td id=\"lastName\">" + lastName);
		newDocument = newDocument.replace("<td id=\"phoneNumber\">", "<td id=\"phoneNumber\">" + phoneNumber);
		
		//concatenate the file name
		String fileName = firstName + lastName + phoneNumber + ".html";
		
		//create the new HTML file
		File contactDocument = new File (fileName);
		FileWriter fw;
		fw = new FileWriter(contactDocument);
		fw.write(newDocument);
		fw.close();

		//get the S3 client
		s3client = getS3Client();
		
		//store the HTML file in S3
		s3client.putObject(bucketName, fileName, contactDocument);
		
		//delete the local file after storing in S3 so it is not retained
		contactDocument.delete();
	}
}
