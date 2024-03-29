package org.metadatacenter.cedar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.csv.LookupSpec;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LookupSpec_Test {

    @Test
    void shouldMatchOntologyWithBranch() {
        var s = "https://bioportal.bioontology.org/ontologies/ABC/?p=classes&conceptid=http://example.org/A";
        Assertions.assertThat(new LookupSpec(s).getOntology()).isEqualTo(Optional.of("https://bioportal.bioontology.org/ontologies/ABC"));
        assertThat(new LookupSpec(s).getBranch()).isEqualTo(Optional.of("http://example.org/A"));
    }

    @Test
    void shouldMatchOntologyWithoutBranch() {
        var s = "https://bioportal.bioontology.org/ontologies/ABC";
        assertThat(new LookupSpec(s).getOntology()).isEqualTo(Optional.of("https://bioportal.bioontology.org/ontologies/ABC"));
        assertThat(new LookupSpec(s).getBranch()).isEmpty();
    }


    @Test
    void shouldMatchOntologyAcronymWithoutBranch() {
        var s = "https://bioportal.bioontology.org/ontologies/ABC";
        assertThat(new LookupSpec(s).getOntologyAcronym()).isEqualTo(Optional.of("ABC"));
        assertThat(new LookupSpec(s).getBranch()).isEmpty();
    }

    @Test
    void shouldNotMatchOntology() {
        var s = "stuff";
        assertThat(new LookupSpec(s).getOntology()).isEmpty();
    }

    @Test
    void shouldNotMatchTermList() {
        var s = "stuff";
        assertThat(new LookupSpec(s).getTermSpecList()).isEmpty();
    }

    @Test
    void shouldMatchSingleTermListWithSpace() {
        var s = "[thelabel] (theiri)";
        assertThat(new LookupSpec(s).getTermSpecList()).contains(new LookupSpec.TermSpec("thelabel", "theiri"));
    }

    @Test
    void shouldMatchSingleTermListWithoutSpace() {
        var s = "[thelabel](theiri)";
        assertThat(new LookupSpec(s).getTermSpecList()).contains(new LookupSpec.TermSpec("thelabel", "theiri"));
    }

    @Test
    void shouldMatchMultipeTermListWithoutSpace() {
        var s = "[thelabel](theiri)\n[thesecondlabel](thesecondiri)";
        assertThat(new LookupSpec(s).getTermSpecList()).contains(new LookupSpec.TermSpec("thelabel", "theiri"));
        assertThat(new LookupSpec(s).getTermSpecList()).contains(new LookupSpec.TermSpec("thesecondlabel", "thesecondiri"));
    }




}