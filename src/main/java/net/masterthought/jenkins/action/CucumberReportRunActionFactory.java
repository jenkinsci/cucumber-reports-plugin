package net.masterthought.jenkins.action;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.jenkins.Messages;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class CucumberReportRunActionFactory extends TransientActionFactory<Run> {

    private static final Logger LOGGER = Logger.getLogger(CucumberReportRunActionFactory.class.getName());

    @Override
    public Class<Run> type() {
        return Run.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Run target) {
        Collection<Action> actions = new ArrayList<>();
        CucumberReportRunAction cucumberReportRunAction = new CucumberReportRunAction(new File(target.getRootDir(), ReportBuilder.BASE_DIRECTORY),
                ReportBuilder.BASE_DIRECTORY, ReportBuilder.HOME_PAGE, CucumberReportProjectAction.ICON_NAME, Messages.SidePanel_DisplayName());

        try {
            cucumberReportRunAction.processDirectory();
            actions.add(cucumberReportRunAction);
        } catch (IOException | NoSuchAlgorithmException ex) {
            LOGGER.log(Level.WARNING, "Exception scanning " + target.getRootDir(), ex);
        }
        return actions;
    }

}
