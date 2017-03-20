package net.masterthought.jenkins.action;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;
import net.masterthought.cucumber.ReportBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Extension
public class CucumberReportProjectActionFactory extends TransientActionFactory<Job> {

    private static final String LAST_COMPLETED_BUILD = "lastCompletedBuild";
    private static final String URL = LAST_COMPLETED_BUILD + "/" + ReportBuilder.BASE_DIRECTORY + "/" + ReportBuilder.HOME_PAGE;

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job target) {
        Collection<Action> actions = new ArrayList<>();
        Run<?, ?> run = target.getLastCompletedBuild();
        if (run != null) {
            List<? extends Action> allActions = run.getAllActions();
            for (Action action : allActions) {
                if (action instanceof CucumberReportRunAction) {
                    actions.add(new CucumberReportProjectAction(URL));
                }
            }
        }

        // none build was completed, report is yet not available
        return actions;
    }
}
