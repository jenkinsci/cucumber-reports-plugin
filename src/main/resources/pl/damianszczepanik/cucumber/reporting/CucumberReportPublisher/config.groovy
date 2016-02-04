package pl.damianszczepanik.cucumber.report.CucumberReportPublisher;

f=namespace(lib.FormTagLib)

f.advanced(field:"help") {

    f.entry(title:_("jsonReportDirectory.name"), description:_("jsonReportDirectory.description"), field:"jsonReportDirectory") {
        f.textbox()
    }
    f.entry(title:_("fileIncludePattern.name"), description:_("fileIncludePattern.description"), field:"fileIncludePattern") {
        f.textbox()
    }
    f.entry(title:_("fileExcludePattern.name"), description:_("fileExcludePattern.description"), field:"fileExcludePattern") {
        f.textbox()
    }

    f.entry(title:_("jenkinsBasePath.name"), description:_("jenkinsBasePath.description"), field:"jenkinsBasePath") {
        f.textbox()
    }

    f.entry(title:_("skippedFails.name"), description:_("skippedFails.description"), field:"skippedFails") {
        f.checkbox()
    }
    f.entry(title:_("pendingFails.name"), description:_("pendingFails.description"), field:"pendingFails") {
        f.checkbox()
    }
    f.entry(title:_("undefinedFails.name"), description:_("undefinedFails.description"), field:"undefinedFails") {
        f.checkbox()
    }
    f.entry(title:_("missingFails.name"), description:_("missingFails.description"), field:"missingFails") {
        f.checkbox()
    }

    f.entry(title:_("ignoreFailedTests.name"), description:_("ignoreFailedTests.description"), field:"ignoreFailedTests") {
        f.checkbox()
    }

    f.entry(title:_("parallelTesting.name"), description:_("parallelTesting.description"), field:"parallelTesting") {
        f.checkbox()
    }
}
