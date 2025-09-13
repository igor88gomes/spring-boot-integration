package com.igorgomes.integration.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

// Svenska: JUnit Platform-runner för Cucumber. Kör alla .feature under classpath:features
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.igorgomes.integration.bdd")
// Svenska: "pretty,summary" ger läsbar output lokalt och i CI
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class CucumberTest {
}
