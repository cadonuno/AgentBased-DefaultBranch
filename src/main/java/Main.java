import executionparameters.ExecutionParameters;
import selenium.ScaProjectUpdater;
import util.Logger;
import util.Workspace;
import util.apihandlers.ApiCaller;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) {
        ExecutionParameters.of(args).ifPresent(Main::execute);
    }

    private static void execute(ExecutionParameters executionParameters) {
        Logger.log("Gathering workspace list");
        List<Workspace> workspaceList = ApiCaller.getAllWorkspaces(executionParameters.getApiCredentials());
        Logger.log("Found " + workspaceList.size() + " Workspaces");
        Logger.currentLevel++;
        workspaceList.forEach(workspace -> {
            Logger.log("Gathering project list for workspace: " + workspace.getName());
            workspace.getProjects().addAll(ApiCaller.getAllProjects(executionParameters.getApiCredentials(), workspace));
            Logger.log("Found " + workspace.getProjects().size() + " Projects");
            Logger.printLine();
        });
        Logger.currentLevel--;

        try {
            new ScaProjectUpdater(
                    executionParameters.getSeleniumDriverName(), executionParameters.getSeleniumDriverLocation(),
                    executionParameters.getVeracodeUsername(), executionParameters.getVeracodePassword())
                    .updateDefaultBranches(workspaceList, executionParameters.getBranchName());
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
