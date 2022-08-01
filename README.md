# Agent-Based SCA - DefaultBranch setter:
This application will change the default branch of all projects visible to the current user

## Requirements:
- Java 8 installed
- A Veracode account with permission to edit a default branch
- An SCA subscription
- API Credentials (ID and Key)
- At least one workspace visible which contains at least one agent-based scan
- Selenium web driver

## How to use:
- Package the application using Maven
- Call the jar by passing the required parameters
- Example call: java -jar **jar name** -vi **veracode id** -vk **veracode key** -vu **veracode username* -vp **veracode password** -dn **selenium driver name (webdriver.gecko.driver recommended)** -dl **Location of the Selenium driver** -b **Branch name to set as default, must be an exact, case-sensitive, match**

## Parameters:
All parameters are mandatory
- Veracode Credentials ID *
  - --veracode_id or -vi
- Veracode Credentials Key *
  - --veracode_key or -vk
- Veracode Username *
  - --veracode_username or -vu
- Veracode Password *
  - --veracode_password or -vp
- Selenium Driver Name *
  - --selenium_driver_name or -dn
- Selenium Driver Location *
  - --selenium_driver_location or -dl
- Branch name *
  - --branch_name or -b
- Debug Mode - default false
  - --debug or -d
- Debug Selenium - default false
  - --debug_selenium or -ds
- Headless mode - default true
  - --headless or -h

