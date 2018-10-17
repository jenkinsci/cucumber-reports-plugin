package net.masterthought.jenkins;

import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.QueryParameter;

import net.masterthought.cucumber.sorting.SortingMethod;

public class CucumberReportDescriptor extends BuildStepDescriptor<Publisher> {

    @Override
    public String getDisplayName() {
        return Messages.Plugin_DisplayName();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    // names must refer to the field name
    public ListBoxModel doFillBuildStatusItems() {
        return new ListBoxModel(
                // default option should be listed first
                new ListBoxModel.Option(Messages.BuildStatus_unchanged(), null),
                new ListBoxModel.Option(Messages.BuildStatus_FAILURE(), Result.FAILURE.toString()),
                new ListBoxModel.Option(Messages.BuildStatus_UNSTABLE(), Result.UNSTABLE.toString()));
    }


    public FormValidation doCheckTrendsLimit(@QueryParameter String value) {
        return isValidNumber(value);
    }

    public FormValidation doCheckFailedStepsNumber(@QueryParameter String value) {
        return isValidNumber(value);
    }

    public FormValidation doCheckSkippedStepsNumber(@QueryParameter String value) {
        return isValidNumber(value);
    }

    public FormValidation doCheckPendingStepsNumber(@QueryParameter String value) {
        return isValidNumber(value);
    }

    public FormValidation doCheckUndefinedStepsNumber(@QueryParameter String value) {
        return isValidNumber(value);
    }

    public FormValidation doCheckFailedScenariosNumber(@QueryParameter String value) {
        return isValidNumber(value);
    }

    public FormValidation doCheckFailedFeaturesNumber(@QueryParameter String value) {
        return isValidNumber(value);
    }

    private static FormValidation isValidNumber(String value) {
        int intValue;
        try {
            intValue = Integer.parseInt(value);
            if (intValue >= 0) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.Configuration_notValidNumberRange());
            }
        } catch (NumberFormatException e) {
            return FormValidation.error(Messages.Configuration_notValidNumber());
        }
    }

    // names must refer to the field name
    public ListBoxModel doFillSortingMethodItems() {
        return new ListBoxModel(
                // default option should be listed first
                new ListBoxModel.Option(Messages.SortingMethod_ALPHABETICAL(), SortingMethod.ALPHABETICAL.name()),
                new ListBoxModel.Option(Messages.SortingMethod_NATURAL(), SortingMethod.NATURAL.name()));
    }
}