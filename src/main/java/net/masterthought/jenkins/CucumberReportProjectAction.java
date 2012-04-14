//package net.masterthought.jenkins;
//
//import hudson.model.AbstractItem;
//import hudson.model.AbstractProject;
//import hudson.model.ProminentProjectAction;
//import hudson.model.Run;
//
//import java.io.File;
//
//public class CucumberReportProjectAction extends CucumberReportBaseAction implements ProminentProjectAction {
//    private final AbstractItem project;
//
//    public CucumberReportProjectAction(AbstractItem project) {
//        super();
//        this.project = project;
//    }
//
//    @Override
//    protected File dir() {
//        if (this.project instanceof AbstractProject) {
//            AbstractProject abstractProject = (AbstractProject) this.project;
//
//            Run run = abstractProject.getLastSuccessfulBuild();
//            if (run != null) {
//                File javadocDir = getBuildArchiveDir(run);
//
//                if (javadocDir.exists()) {
//                    return javadocDir;
//                }
//            }
//        }
//
//        return getProjectArchiveDir(this.project);
//    }
//
//    @Override
//    protected String getTitle() {
//        return this.project.getDisplayName() + " html2";
//    }
//}