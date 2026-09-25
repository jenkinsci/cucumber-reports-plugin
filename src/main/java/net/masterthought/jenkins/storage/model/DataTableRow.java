package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a row in a BDD Gherkin DataTable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTableRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> cells = new ArrayList<>();

    public DataTableRow() {
    }

    public DataTableRow(List<String> cells) {
        this.cells = cells;
    }

    public List<String> getCells() {
        return cells;
    }

    public void setCells(List<String> cells) {
        this.cells = cells != null ? cells : new ArrayList<>();
    }
}
