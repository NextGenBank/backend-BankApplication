package com.nextgenbank.backend;
import com.nextgenbank.backend.config.TestSecurityConfig;
import io.cucumber.junit.platform.engine.Cucumber;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.context.annotation.Import;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@Cucumber
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.nextgenbank.backend.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @ignore")
@Import(TestSecurityConfig.class)
public class CucumberTest {
}
