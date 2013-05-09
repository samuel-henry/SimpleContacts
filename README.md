Create a console-driven interface to allow the user to 
 - list contacts
 - retrieve details about a selected contact
 - edit details
 - create new contacts

All contact information should be stored in SimpleDB

Enhance your contact record so it can handle the following:
 - First name
 - Last name
 - An arbitrary number of telephone numbers, each with a label (e.g., cell, home, office, etc.)
 - An arbitrary number of email addresses, each with a label (e.g., personal, work, school, etc.)
 - An address (street address, city, state, 5-digit zip code)
 - An arbitrary number of tags (e.g., friend, family, work, vendor, etc). 
 - Birthday
All values, except first name, are optional
Tags and labels can be created as needed per contact. 

The user should be able to enter search criteria as described below, and receive a list of all contacts matching the criteria
 - First name starts with <characters>
 - Last name starts with <characters>
 - State = <two-character state code>
 - zip = <five-digit zip code>
 - tag = <a specific value> or <all of a set of delimited values - e.g., friend|co-worker finds everyone who is both a friend and a coworker>
birthday before or after a specific date, or between two dates.
These are separate searches. You do not need to combine them (e.g., friends in IL)
When a record has been changed (and edits are complete), a new HTML document should be generated and stored in S3. 
The HTML format must be updated to accomodate the additional fields.
The HTML format should not display headers for fields with no data.
