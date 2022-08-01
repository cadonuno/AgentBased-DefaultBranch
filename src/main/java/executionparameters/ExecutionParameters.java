package executionparameters;

import selenium.WebDriverProvider;
import util.ApiCredentials;
import util.Logger;

import static executionparameters.Parameters.*;

import java.util.Optional;

public class ExecutionParameters {
    private final ApiCredentials apiCredentials;
    private final String veracodeUsername;
    private final String veracodePassword;
    private final String branchName;
    private final String seleniumDriverName;
    private final String seleniumDriverLocation;

    protected ExecutionParameters(ApiCredentials apiCredentials,
                                  String veracodeUsername, String veracodePassword,
                                  String seleniumDriverName, String seleniumDriverLocation,
                                  String branchName) {
        validateInput(veracodeUsername, veracodePassword, seleniumDriverName, seleniumDriverLocation, branchName);
        this.veracodeUsername = veracodeUsername;
        this.veracodePassword = veracodePassword;
        this.branchName = branchName;
        this.seleniumDriverName = seleniumDriverName;
        this.seleniumDriverLocation = seleniumDriverLocation;
        this.apiCredentials = apiCredentials;
    }

    private void validateInput(String veracodeUsername, String veracodePassword, String seleniumDriverName,
                               String seleniumDriverLocation, String branchName) {
        validateSingleInput(veracodeUsername, "Veracode username",
                VERACODE_USERNAME_FULL_ARGUMENT, VERACODE_USERNAME_SIMPLIFIED_ARGUMENT);
        validateSingleInput(veracodePassword, "Veracode password",
                VERACODE_PASSWORD_FULL_ARGUMENT, VERACODE_PASSWORD_SIMPLIFIED_ARGUMENT);
        validateSingleInput(seleniumDriverName, "Selenium driver name",
                SELENIUM_DRIVER_NAME_FULL_ARGUMENT, SELENIUM_DRIVER_NAME_SIMPLIFIED_ARGUMENT);
        validateSingleInput(seleniumDriverLocation, "Selenium driver location",
                SELENIUM_DRIVER_LOCATION_FULL_ARGUMENT, SELENIUM_DRIVER_LOCATION_SIMPLIFIED_ARGUMENT);
        validateSingleInput(branchName, "Branch name",
                BRANCH_NAME_FULL_ARGUMENT, BRANCH_NAME_SIMPLIFIED_ARGUMENT);
    }

    private void validateSingleInput(String veracodeUsername, String fullName,
                                     String fullArgument, String simplifiedArgument) {
        if (veracodeUsername == null || veracodeUsername.isEmpty()) {
            throw new IllegalArgumentException(fullName + " argument is mandatory (" + fullArgument + ", " + simplifiedArgument + ")");
        }
    }

    public static Optional<ExecutionParameters> of(String[] commandLineArguments) {
        Logger.log("Parsing Execution Parameters");
        Optional<ExecutionParameters> executionParameters =
                Optional.of(parseParameters(new ParameterParser(commandLineArguments)));
        Logger.log("Finished parsing Execution Parameters");
        return executionParameters;
    }

    private static ExecutionParameters parseParameters(
            ParameterParser parameterParser) {
        Logger.isDebug = Optional.ofNullable(parameterParser.getParameterAsString("--debug", "-d"))
                .filter("true"::equals)
                .isPresent();
        Logger.isDebugSelenium = Optional.ofNullable(parameterParser.getParameterAsString("--debug_selenium", "-ds"))
                .filter("true"::equals)
                .isPresent();
        WebDriverProvider.isHeadless = Optional.ofNullable(parameterParser.getParameterAsString("--headless", "-h"))
                .map("true"::equals)
                .orElse(true);
        return new ExecutionParameters(
                new ApiCredentials(
                        parameterParser.getParameterAsString(VERACODE_ID_FULL_ARGUMENT, VERACODE_ID_SIMPLIFIED_ARGUMENT),
                        parameterParser.getParameterAsString(VERACODE_KEY_FULL_ARGUMENT, VERACODE_KEY_SIMPLIFIED_ARGUMENT)),
                parameterParser.getParameterAsString(VERACODE_USERNAME_FULL_ARGUMENT, VERACODE_USERNAME_SIMPLIFIED_ARGUMENT),
                parameterParser.getParameterAsString(VERACODE_PASSWORD_FULL_ARGUMENT, VERACODE_PASSWORD_SIMPLIFIED_ARGUMENT),
                parameterParser.getParameterAsString(SELENIUM_DRIVER_NAME_FULL_ARGUMENT, SELENIUM_DRIVER_NAME_SIMPLIFIED_ARGUMENT),
                parameterParser.getParameterAsString(SELENIUM_DRIVER_LOCATION_FULL_ARGUMENT, SELENIUM_DRIVER_LOCATION_SIMPLIFIED_ARGUMENT),
                parameterParser.getParameterAsString(BRANCH_NAME_FULL_ARGUMENT, BRANCH_NAME_SIMPLIFIED_ARGUMENT));
    }

    public ApiCredentials getApiCredentials() {
        return apiCredentials;
    }

    public String getSeleniumDriverLocation() {
        return seleniumDriverLocation;
    }

    public String getSeleniumDriverName() {
        return seleniumDriverName;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getVeracodePassword() {
        return veracodePassword;
    }

    public String getVeracodeUsername() {
        return veracodeUsername;
    }
}
