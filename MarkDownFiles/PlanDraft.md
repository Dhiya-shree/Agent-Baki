Let's plan now .
Teck stack to use for backend : Java , Spring 
Front end : Java Thymeleaf or React  

User is a developer who has to work on multiple code fix .
User input : List of application name and local repository link 

Phase 1 : Collecting data 
Flow 1 : Using outlook 
1) Find how to access user outlook mail 
2) Find  way to download content and logs for mails with subject like Issues/Ticket/Incident
3) If application name / logs not present , find the best way to reply back to mail with message -"Application name/logs not present"
Flow 2: Jira incident 
1) Find how to access user jira 
2) Find  way to download content and logs for incident 
3) If application name / logs not present , find the best way to reply back in incident with message -"Application name/logs not present"

Phase 2 : Analysing Data 
1) Based on application name , create new entry in ApplicationTable name application_data - application_id
2) Create below tables:
   1) fix_for_issues - Fix_id , application_id ,issue_summary(250 characters) , Code class name , Code line ,github PR ,issue_status 
   2) Mail_issues_table - mail_id , Fix_id from Fix  ,application_id from Application table ,replied (Y/N),log_location,created_time, (Add any other columns for maintaining and triggering mail response )
   3) Jira_issues_table - jira_id , Fix_id from Fix  ,application_id from Application table ,replied (Y/N), ,log_location,created_time,(Add any other columns for maintaining and triggering Jira response) 
   Maintain Many to one relation for Mail-->fix and Jira-->Fix 
   4) Use folder structure for log location C:\Users\{application name}\issues\{mail_id/jira_id}\logs
3) Checks ways on how to analyse the logs using watsonx ai or ibm bob ide 
4) Find the exact line in code causing issue 
5) LookUp in fix - Code line , if issue is already mentioned .
      1) If yes , update the mail_id/Jira_id with existing fix_id 
      2) If no , create new entry in fix table and update the mail_id/Jira_id in Mail_issues_table/Jira_issues_table with new fix_id 
6) Create a new entry in mail/jira table with application_id and fix_id 


Phase 3:Creating PR / Checkpoint 
1) In thymeleaf/react UI show user list of  line causing issue under application name 
2) Give user text field and 2 button on clicking in a issue : 
    a) Ignore - Text : Reason 
    b) Issue in progress - PR number 
    c) DB fix - Fix number 

Phase 4 : Batch Reply 
Create a batch job on Mail_issues_table and Jira_issues_table table for every 15 mins , for replied = N check if there is a fix_id 
if yes 
1) Trigger output reply response for mail and jira trigger to add comment with fix_id table values 
2) Delete the logs and files under C:\Users\{application name}\issues\{mail_id/jira_id} 
3) Update the replied = Y in mail/jira table 




