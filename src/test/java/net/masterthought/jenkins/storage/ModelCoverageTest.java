package net.masterthought.jenkins.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.masterthought.jenkins.storage.model.Attachment;
import net.masterthought.jenkins.storage.model.CucumberReportPayload;
import net.masterthought.jenkins.storage.model.DataTableRow;
import net.masterthought.jenkins.storage.model.FeatureResult;
import net.masterthought.jenkins.storage.model.HookResult;
import net.masterthought.jenkins.storage.model.ReportSummary;
import net.masterthought.jenkins.storage.model.ScenarioResult;
import net.masterthought.jenkins.storage.model.StepResult;
import org.junit.jupiter.api.Test;

public class ModelCoverageTest {

    @Test
    public void testAttachmentModelBranches() {
        Attachment att1 = new Attachment();
        att1.setName("att1");
        att1.setMimeType("text/plain");
        att1.setData("sample data");
        att1.setSizeBytes(11);
        att1.setExternal(true);
        att1.setExternalFileId("att-123.dat");
        att1.setTruncated(true);
        att1.setPreview("sample...");

        assertEquals("att1", att1.getName());
        assertEquals("text/plain", att1.getMimeType());
        assertEquals("sample data", att1.getData());
        assertEquals(11, att1.getSizeBytes());
        assertTrue(att1.isExternal());
        assertEquals("att-123.dat", att1.getExternalFileId());
        assertTrue(att1.isTruncated());
        assertEquals("sample...", att1.getPreview());

        // Test constructor with null data branch
        Attachment attNullData = new Attachment("attNull", "image/png", null);
        assertEquals(0, attNullData.getSizeBytes());
        assertNull(attNullData.getData());

        // Test constructor with non-null data branch
        Attachment attNonNull = new Attachment("attNonNull", "image/png", "base64data");
        assertEquals(10, attNonNull.getSizeBytes());
    }

    @Test
    public void testCucumberReportPayloadBranches() {
        CucumberReportPayload payload = new CucumberReportPayload();
        payload.setBuildDisplayName("Build #5");
        payload.setBuildUrl("job/demo/5");
        payload.setEmbedFullAttachments(true);
        payload.setAttachmentThresholdKB(250);

        assertEquals("Build #5", payload.getBuildDisplayName());
        assertEquals("job/demo/5", payload.getBuildUrl());
        assertTrue(payload.isEmbedFullAttachments());
        assertEquals(250, payload.getAttachmentThresholdKB());

        // Test null features branch
        payload.setFeatures(null);
        assertNotNull(payload.getFeatures());
        assertTrue(payload.getFeatures().isEmpty());

        // Test non-null features branch
        payload.setFeatures(Collections.singletonList(new FeatureResult()));
        assertEquals(1, payload.getFeatures().size());
    }

    @Test
    public void testHookResultBranches() {
        HookResult hook = new HookResult();
        hook.setType("before");
        hook.setStatus("failed");
        hook.setDuration(1000L);
        hook.setErrorMessage("Setup error");
        hook.setErrorStackTrace("Trace...");

        assertEquals("before", hook.getType());
        assertEquals("failed", hook.getStatus());
        assertEquals(1000L, hook.getDuration());
        assertEquals("Setup error", hook.getErrorMessage());
        assertEquals("Trace...", hook.getErrorStackTrace());

        // Test null attachments branch
        hook.setAttachments(null);
        assertNotNull(hook.getAttachments());
        assertTrue(hook.getAttachments().isEmpty());

        // Test non-null attachments branch
        hook.setAttachments(Collections.singletonList(new Attachment()));
        assertEquals(1, hook.getAttachments().size());
    }

    @Test
    public void testStepResultBranches() {
        StepResult step = new StepResult();
        step.setKeyword("Given ");
        step.setName("the test starts");
        step.setLine(15);
        step.setStatus("passed");
        step.setDuration(5000L);
        step.setErrorMessage(null);
        step.setErrorStackTrace(null);
        step.setDocString("doc string content");

        assertEquals("Given ", step.getKeyword());
        assertEquals("the test starts", step.getName());
        assertEquals(15, step.getLine());
        assertEquals("passed", step.getStatus());
        assertEquals(5000L, step.getDuration());
        assertNull(step.getErrorMessage());
        assertNull(step.getErrorStackTrace());
        assertEquals("doc string content", step.getDocString());

        // Null rows branch
        step.setRows(null);
        assertNotNull(step.getRows());
        assertTrue(step.getRows().isEmpty());

        // Non-null rows branch
        step.setRows(Collections.singletonList(new DataTableRow()));
        assertEquals(1, step.getRows().size());

        // Null attachments branch
        step.setAttachments(null);
        assertNotNull(step.getAttachments());
        assertTrue(step.getAttachments().isEmpty());

        // Non-null attachments branch
        step.setAttachments(Collections.singletonList(new Attachment()));
        assertEquals(1, step.getAttachments().size());
    }

    @Test
    public void testScenarioResultBranches() {
        ScenarioResult scenario = new ScenarioResult();
        scenario.setId("sc-1");
        scenario.setName("Scenario 1");
        scenario.setDescription("Scenario description");
        scenario.setKeyword("Scenario");
        scenario.setLine(20);
        scenario.setFeatureName("Feature 1");
        scenario.setErrorMessage("err");
        scenario.setErrorStackTrace("stack");

        assertEquals("sc-1", scenario.getId());
        assertEquals("Scenario 1", scenario.getName());
        assertEquals("Scenario description", scenario.getDescription());
        assertEquals("Scenario", scenario.getKeyword());
        assertEquals(20, scenario.getLine());
        assertEquals("Feature 1", scenario.getFeatureName());
        assertEquals("err", scenario.getErrorMessage());
        assertEquals("stack", scenario.getErrorStackTrace());

        // Test null collections handling
        scenario.setTags(null);
        assertNotNull(scenario.getTags());
        scenario.setTags(Arrays.asList("tag1", "tag2"));
        assertEquals(2, scenario.getTags().size());

        scenario.setSteps(null);
        assertNotNull(scenario.getSteps());

        scenario.setBeforeHooks(null);
        assertNotNull(scenario.getBeforeHooks());
        scenario.setBeforeHooks(Collections.singletonList(new HookResult()));
        assertEquals(1, scenario.getBeforeHooks().size());

        scenario.setAfterHooks(null);
        assertNotNull(scenario.getAfterHooks());
        scenario.setAfterHooks(Collections.singletonList(new HookResult()));
        assertEquals(1, scenario.getAfterHooks().size());

        // Test status computation branches
        // 1. Empty steps -> passed
        scenario.setSteps(Collections.emptyList());
        scenario.setBeforeHooks(Collections.emptyList());
        scenario.setAfterHooks(Collections.emptyList());
        assertEquals("passed", scenario.computeStatus());

        // 2. Failed step -> failed
        StepResult failedStep = new StepResult();
        failedStep.setStatus("failed");
        scenario.setSteps(Collections.singletonList(failedStep));
        assertEquals("failed", scenario.computeStatus());

        // 3. Skipped step -> skipped
        StepResult skippedStep = new StepResult();
        skippedStep.setStatus("skipped");
        scenario.setSteps(Collections.singletonList(skippedStep));
        assertEquals("skipped", scenario.computeStatus());

        // 4. Failed before hook -> failed
        scenario.setSteps(Collections.emptyList());
        HookResult failedHook = new HookResult();
        failedHook.setStatus("failed");
        scenario.setBeforeHooks(Collections.singletonList(failedHook));
        assertEquals("failed", scenario.computeStatus());

        // 5. Failed after hook -> failed
        scenario.setBeforeHooks(Collections.emptyList());
        scenario.setAfterHooks(Collections.singletonList(failedHook));
        assertEquals("failed", scenario.computeStatus());

        // Duration computation
        StepResult timedStep = new StepResult();
        timedStep.setDuration(100L);
        scenario.setSteps(Collections.singletonList(timedStep));
        HookResult timedHook = new HookResult();
        timedHook.setDuration(50L);
        scenario.setBeforeHooks(Collections.singletonList(timedHook));
        scenario.setAfterHooks(Collections.singletonList(timedHook));
        assertEquals(200L, scenario.computeDuration());
    }

    @Test
    public void testFeatureResultBranches() {
        FeatureResult feature = new FeatureResult();
        feature.setId("feat-1");
        feature.setName("Feature 1");
        feature.setUri("features/login.feature");
        feature.setDescription("Feature description");
        feature.setKeyword("Feature");
        feature.setLine(1);
        feature.setStatus("passed");
        feature.setDuration(1234L);

        assertEquals("feat-1", feature.getId());
        assertEquals("Feature 1", feature.getName());
        assertEquals("features/login.feature", feature.getUri());
        assertEquals("Feature description", feature.getDescription());
        assertEquals("Feature", feature.getKeyword());
        assertEquals(1, feature.getLine());
        assertEquals("passed", feature.getStatus());
        assertEquals(1234L, feature.getDuration());

        // Null collections handling
        feature.setTags(null);
        assertNotNull(feature.getTags());
        feature.setTags(Collections.singletonList("fast"));
        assertEquals(1, feature.getTags().size());

        feature.setScenarios(null);
        assertNotNull(feature.getScenarios());

        // Compute status branches
        // 1. Empty scenarios -> passed
        feature.setScenarios(Collections.emptyList());
        assertEquals("passed", feature.computeStatus());

        // 2. Failed scenario -> failed
        ScenarioResult failedSc = new ScenarioResult();
        failedSc.setStatus("failed");
        feature.setScenarios(Collections.singletonList(failedSc));
        assertEquals("failed", feature.computeStatus());

        // 3. Skipped scenario -> skipped
        ScenarioResult skippedSc = new ScenarioResult();
        skippedSc.setStatus("skipped");
        feature.setScenarios(Collections.singletonList(skippedSc));
        assertEquals("skipped", feature.computeStatus());

        // 4. Passed scenario -> passed
        ScenarioResult passedSc = new ScenarioResult();
        passedSc.setStatus("passed");
        feature.setScenarios(Collections.singletonList(passedSc));
        assertEquals("passed", feature.computeStatus());

        // Compute duration
        passedSc.setDuration(300L);
        feature.setScenarios(Collections.singletonList(passedSc));
        assertEquals(300L, feature.computeDuration());
    }

    @Test
    public void testReportSummaryBranches() {
        ReportSummary summary = new ReportSummary();
        summary.setTotalFeatures(10);
        summary.setPassedFeatures(8);
        summary.setFailedFeatures(1);
        summary.setSkippedFeatures(1);

        summary.setTotalScenarios(20);
        summary.setPassedScenarios(16);
        summary.setFailedScenarios(2);
        summary.setSkippedScenarios(2);

        summary.setTotalSteps(100);
        summary.setPassedSteps(90);
        summary.setFailedSteps(5);
        summary.setSkippedSteps(5);

        summary.setTotalDuration(5000000000L);
        summary.setTotalAttachments(4);
        summary.setAllTags(Arrays.asList("tagA", "tagB"));

        assertEquals(10, summary.getTotalFeatures());
        assertEquals(8, summary.getPassedFeatures());
        assertEquals(1, summary.getFailedFeatures());
        assertEquals(1, summary.getSkippedFeatures());

        assertEquals(20, summary.getTotalScenarios());
        assertEquals(16, summary.getPassedScenarios());
        assertEquals(2, summary.getFailedScenarios());
        assertEquals(2, summary.getSkippedScenarios());

        assertEquals(100, summary.getTotalSteps());
        assertEquals(90, summary.getPassedSteps());
        assertEquals(5, summary.getFailedSteps());
        assertEquals(5, summary.getSkippedSteps());

        assertEquals(5000000000L, summary.getTotalDuration());
        assertEquals(4, summary.getTotalAttachments());
        assertEquals(2, summary.getAllTags().size());
        assertEquals(80.0, summary.getPassPercentage(), 0.01);
        assertNotNull(summary.getFormattedDuration());

        // Null tags branch
        summary.setAllTags(null);
        assertNotNull(summary.getAllTags());

        // 0 scenarios pass percentage branch
        ReportSummary emptySummary = new ReportSummary();
        assertEquals(100.0, emptySummary.getPassPercentage(), 0.01);

        // Formatted duration branches
        assertEquals("0ms", ReportSummary.formatDuration(-10L));
        assertEquals("0ms", ReportSummary.formatDuration(0L));
        assertEquals("500ms", ReportSummary.formatDuration(500_000_000L));
        assertEquals("5.0s", ReportSummary.formatDuration(5_000_000_000L));
        assertEquals("2m 5s", ReportSummary.formatDuration(125_000_000_000L));
        assertEquals("2h 0m", ReportSummary.formatDuration(7200_000_000_000L));

        // Formatted duration with manual setter
        emptySummary.setFormattedDuration("05s 000ms");
        assertEquals("05s 000ms", emptySummary.getFormattedDuration());
    }

    @Test
    public void testDataTableRowBranches() {
        DataTableRow row = new DataTableRow();
        row.setCells(null);
        assertNotNull(row.getCells());
        assertTrue(row.getCells().isEmpty());

        DataTableRow rowWithCells = new DataTableRow(Arrays.asList("col1", "col2"));
        assertEquals(2, rowWithCells.getCells().size());
        assertEquals("col1", rowWithCells.getCells().get(0));
    }
}
