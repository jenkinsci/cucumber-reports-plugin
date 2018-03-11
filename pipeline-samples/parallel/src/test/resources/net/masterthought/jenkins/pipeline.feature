Feature: Report generation in Jenkins pipelines

  Scenario: Pipeline generates an html report
    Given a Java based test
    When it is run via pipeline
    Then a pretty report is generated
