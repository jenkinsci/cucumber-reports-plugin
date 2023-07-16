package net.masterthought.jenkins;

import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.masterthought.cucumber.sorting.SortingMethod;
import org.kohsuke.stapler.QueryParameter;

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
                new ListBoxModel.Option(Messages.BuildStatus_unchanged(), "UNCHANGED"),
                new ListBoxModel.Option(Messages.BuildStatus_FAILURE(), Result.FAILURE.toString()),
                new ListBoxModel.Option(Messages.BuildStatus_UNSTABLE(), Result.UNSTABLE.toString()));
    }


    public FormValidation doCheckTrendsLimit(@QueryParameter String value) {
        return isValidInteger(value);
    }

    public FormValidation doCheckFailedStepsNumber(@QueryParameter String value) {
        return isValidInteger(value);
    }

    public FormValidation doCheckSkippedStepsNumber(@QueryParameter String value) {
        return isValidInteger(value);
    }

    public FormValidation doCheckPendingStepsNumber(@QueryParameter String value) {
        return isValidInteger(value);
    }

    public FormValidation doCheckUndefinedStepsNumber(@QueryParameter String value) {
        return isValidInteger(value);
    }

    public FormValidation doCheckFailedScenariosNumber(@QueryParameter String value) {
        return isValidInteger(value);
    }

    public FormValidation doCheckFailedFeaturesNumber(@QueryParameter String value) {
        return isValidInteger(value);
    }

    private static FormValidation isValidInteger(String value) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue == -1) {
                return FormValidation.warning(Messages.Configuration_skipValidation());
            }
            if (intValue >= 0) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.Configuration_notValidNumber());
            }
        } catch (NumberFormatException e) {
            return FormValidation.error(Messages.Configuration_notValidInteger());
        }
    }


    public FormValidation doCheckFailedStepsPercentage(@QueryParameter String value) {
        return isValidPercentage(value);
    }

    public FormValidation doCheckSkippedStepsPercentage(@QueryParameter String value) {
        return isValidPercentage(value);
    }

    public FormValidation doCheckPendingStepsPercentage(@QueryParameter String value) {
        return isValidPercentage(value);
    }

    public FormValidation doCheckUndefinedStepsPercentage(@QueryParameter String value) {
        return isValidPercentage(value);
    }

    public FormValidation doCheckFailedScenariosPercentage(@QueryParameter String value) {
        return isValidPercentage(value);
    }

    public FormValidation doCheckFailedFeaturesPercentage(@QueryParameter String value) {
        return isValidPercentage(value);
    }

    private static FormValidation isValidPercentage(String value) {
        double doubleValue;
        try {
            doubleValue = Double.parseDouble(value);
            if (doubleValue >= 0 && doubleValue <= 100) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.Configuration_notValidInteger());
            }
        } catch (NumberFormatException e) {
            return FormValidation.error(Messages.Configuration_notValidPercentage());
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