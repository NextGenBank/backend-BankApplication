package com.nextgenbank.backend;
import io.cucumber.junit.platform.engine.Cucumber;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@Cucumber
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.nextgenbank.backend.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty") //pretty makes the console output more readable with colored text
public class CucumberTest {
}
