package net.masterthought.jenkins;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * Created by Jakob Fels on 14.07.17.
 */
@RunWith(Cucumber.class)
@CucumberOptions(glue = "net.masterthought.jenkins", plugin = "json:target/cucumber-declarative.json")
public class CucumberRunnerTest {
}
