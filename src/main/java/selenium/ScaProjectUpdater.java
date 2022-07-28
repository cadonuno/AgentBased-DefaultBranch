package selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import util.Logger;
import util.Project;
import util.Workspace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScaProjectUpdater {
    private static final String AGENT_BASED_BASE_URL = "https://sca.analysiscenter.veracode.com";
    private static final String LOGIN_URL = "https://web.analysiscenter.veracode.com/login/";
    private static final String USERNAME_FIELD_ID = "okta-signin-username";
    private static final String PASSWORD_FIELD_ID = "okta-signin-password";
    private static final String LOGIN_BUTTON_ID = "okta-signin-submit";
    private static final String USER_NAME_ICON_ID = "icon_user";
    private static final String SETTINGS_BUTTON_CSS_SELECTOR = ".link--obvious > .font--16";
    private static final String SHOW_BRANCHES_BUTTON_XPATH = "//div[@class='col-1-3 inline-block']/div/div/div/div/div[@class='css-1wy0on6 srcclr-react-select__indicators']";
    private static final String BRANCH_SELECT_OPTION_XPATH = "//div[@class='css-15k3avv srcclr-react-select__menu']";
    private static final String SAVE_BUTTON_XPATH = "//button[text()='Save']";
    private static final int MAX_ATTEMPTS_PER_PROJECT = 10;
    public static final String AGENT_BASED_SCAN_SETTINGS_BUTTON_XPATH = "//div[@data-automation-id='AgentBasedScanSettings-Button']";

    private final String seleniumDriverName;
    private final String seleniumDriverLocation;
    private final String veracodeUsername;
    private final String veracodePassword;
    private final List<Project> projectsNotUpdated = new ArrayList<>();
    private WebDriver webDriver;

    public ScaProjectUpdater(String seleniumDriverName,
                             String seleniumDriverLocation,
                             String veracodeUsername,
                             String veracodePassword) {
        this.seleniumDriverName = seleniumDriverName;
        this.seleniumDriverLocation = seleniumDriverLocation;
        this.veracodeUsername = veracodeUsername;
        this.veracodePassword = veracodePassword;
    }

    public void updateDefaultBranches(List<Workspace> workspaceList, String branchName) throws TimeoutException {
        System.setProperty(seleniumDriverName, seleniumDriverLocation);
        if (!Logger.isDebugSelenium) {
            System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true");
            System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        }
        webDriver = WebDriverProvider.getDriver(seleniumDriverName);
        try {
            loginToPlatform();
            workspaceList.forEach(workspace -> updateDefaultBranchesForWorkspace(branchName, workspace));
        } finally {
            webDriver.quit();
        }
        if (!projectsNotUpdated.isEmpty()) {
            Logger.log("Unable to update the following projects: ");
            Logger.currentLevel++;
            projectsNotUpdated.forEach(project -> {
                Logger.log("- " + project.getFullName());
                Logger.currentLevel++;
                Logger.log(project.getIssueOnUpdate());
                Logger.currentLevel--;
            });
            Logger.currentLevel--;
        }
    }

    private void updateDefaultBranchesForWorkspace(String branchName, Workspace workspace) {
        if (workspace.getProjects().isEmpty()) {
            Logger.log("Skipping workspace " + workspace.getName() + " as it has no projects");
            Logger.printLine();
            return;
        }
        Logger.log("Updating workspace: " + workspace.getName());
        Logger.currentLevel++;
        workspace.getProjects().forEach(project -> {
            Logger.log("- Updating project: " + project.getName());
            Logger.currentLevel++;
            updateDefaultBranchForProject(project, branchName, 0);
            Logger.printLine();
            Logger.currentLevel--;
        });
        Logger.currentLevel--;
        Logger.printLine();
    }

    public void updateDefaultBranchForProject(Project project, String branchName, int attempt) {
        try {
            if (!isLoggedIn()) {
                Logger.debug("Session expired, trying to log back in");
                loginToPlatform();
            }
            String projectUrl = AGENT_BASED_BASE_URL + "/workspaces/" + project.getWorkspace().getSiteId() +
                    "/projects/" + project.getSiteId() + "/issues";
            webDriver.get(projectUrl);
            openSettingsMenu();
            WebElement branchSelectOption =
                    SeleniumHelper.getElement(webDriver, By.xpath(BRANCH_SELECT_OPTION_XPATH)).orElse(null);
            if (branchSelectOption == null
                    || !SeleniumHelper.hasElementRightNow(branchSelectOption, By.xpath("..//div[text()='" + branchName + "']"))) {
                logMissingBranchName(project, branchName, branchSelectOption);
                return;
            }
            SeleniumHelper.clickElement(webDriver, branchSelectOption, By.xpath("..//div[text()='" + branchName + "']"));
            SeleniumHelper.clickElement(webDriver, By.xpath(SAVE_BUTTON_XPATH));
            Logger.log("Successfully set default branch to '" + branchName + "' for project: " + project.getName());
        } catch (TimeoutException e) {
            handleTimeout(project, branchName, attempt, e);
        }
    }

    private void handleTimeout(Project project, String branchName, int attempt, TimeoutException e) {
        if (attempt > MAX_ATTEMPTS_PER_PROJECT) {
            setProjectAsFailed(project, e);
        } else {
            logAndRetry(project, branchName, attempt);
        }
    }

    private void openSettingsMenu() throws TimeoutException {
        SeleniumHelper.waitForElementPresentAndClickIt(webDriver, By.cssSelector(SETTINGS_BUTTON_CSS_SELECTOR));
        SeleniumHelper.waitForElementPresentAndClickIt(webDriver, By.xpath(SHOW_BRANCHES_BUTTON_XPATH));
        waitForLoad();
    }

    private void logMissingBranchName(Project project, String branchName, WebElement branchSelectOption) {
        if (branchSelectOption != null && branchSelectOption.getAttribute("innerHTML") != null) {
            project.setIssueOnUpdate(
                    "Couldn't find branch named " + branchName + " for project: " + project.getName() +
                    "\nFound the following options: " + branchSelectOption.getText().replace("\n", ", "));

        }
        Logger.log("Unable to update project: " + project.getName());
        projectsNotUpdated.add(project);
    }

    private void setProjectAsFailed(Project project, TimeoutException e) {
        projectsNotUpdated.add(project);
        Logger.log("Unable to update project: " + project.getName());
        project.setIssueOnUpdate("Unable to update project: " + project.getName());
        Logger.debug(() -> {
            project.concatenateIssue("Currently on URL: " + webDriver.getCurrentUrl());
            project.concatenateIssue("With page source: " + webDriver.getPageSource());
        });
        Logger.debug(e::printStackTrace);
    }

    private void logAndRetry(Project project, String branchName, int attempt) {
        Logger.log("Error trying to update project: " + project.getName());
        Logger.log("Will retry after 1 second");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
            //nothing to do here
        }
        Logger.log("Retrying (" + attempt + "/" + MAX_ATTEMPTS_PER_PROJECT + ")");
        updateDefaultBranchForProject(project, branchName, ++attempt);
    }

    private boolean isLoggedIn() {
        webDriver.get("https://sca.analysiscenter.veracode.com/portfolio");
        return SeleniumHelper.hasElement(webDriver, By.xpath(AGENT_BASED_SCAN_SETTINGS_BUTTON_XPATH));
    }

    private void waitForLoad() throws TimeoutException {
        boolean hasLoaded = false;
        Instant start = Instant.now();
        while (!hasLoaded) {
            WebElement branchSelectOption =
                    SeleniumHelper.getElement(webDriver, By.xpath(BRANCH_SELECT_OPTION_XPATH)).orElse(null);
            if (branchSelectOption != null) {
                hasLoaded = !SeleniumHelper.hasElementRightNow(branchSelectOption, By.xpath("..//div[text()='Loading...']"));
            }
            if (!hasLoaded) {
                SeleniumHelper.checkTimeout(start, webDriver);
            }
        }
    }

    private void loginToPlatform() throws TimeoutException {
        Logger.log("Logging into the Veracode platform");
        webDriver.get(LOGIN_URL);
        webDriver.manage().window().setSize(new Dimension(1920, 1080));
        SeleniumHelper.waitForElementPresent(webDriver, By.id(LOGIN_BUTTON_ID));
        webDriver.findElement(By.id(USERNAME_FIELD_ID)).sendKeys(veracodeUsername);
        webDriver.findElement(By.id(PASSWORD_FIELD_ID)).sendKeys(veracodePassword);
        SeleniumHelper.clickElement(webDriver, By.id(LOGIN_BUTTON_ID));
        SeleniumHelper.waitForElementPresent(webDriver, By.id(USER_NAME_ICON_ID));
        Logger.log("Logged into the Veracode platform");
        Logger.debug("Landed on URL: " + webDriver.getCurrentUrl());
    }
}
